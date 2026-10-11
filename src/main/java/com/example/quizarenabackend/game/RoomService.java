package com.example.quizarenabackend.game;

import com.example.quizarenabackend.exceptions.*;
import com.example.quizarenabackend.game.dtos.CreateRoomResponse;
import com.example.quizarenabackend.game.entities.Player;
import com.example.quizarenabackend.game.entities.Room;
import com.example.quizarenabackend.game.entities.RoomState;
import com.example.quizarenabackend.game.exceptions.*;
import com.example.quizarenabackend.quiz.QuizRepository;
import com.example.quizarenabackend.quiz.entities.Quiz;
import com.example.quizarenabackend.quiz.exceptons.NoQuestionsQuizException;
import com.example.quizarenabackend.quiz.exceptons.QuizNotFoundException;
import com.example.quizarenabackend.ws.MessageSender;
import com.example.quizarenabackend.ws.messages.ClientMessage;
import com.example.quizarenabackend.ws.messages.ServerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    private static final int MAX_PLAYERS = 50;
    private static final int MAX_NICK_LENGTH = 20;
    private static final String ATTR_ROOM = "roomCode";
    private static final String ATTR_PLAYER = "playerId";

    private final QuizRepository quizRepository;
    private final MessageSender sender;

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    // REST
    @Transactional(readOnly = true)
    public CreateRoomResponse createRoom(Long quizId, String hostNick) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(
                ()->new QuizNotFoundException(quizId)
        );
        if (quiz.getQuestions().isEmpty()) {
            throw new NoQuestionsQuizException(quiz.getTitle());
        }
        // Lazy loading of questions and answer options
        quiz.getQuestions().forEach(q->q.getAnswerOptions().size());
        Player host = new Player(hostNick.trim());
        host.setConnected(true);

        Room room;
        do {
            room = new Room(randomCode(), quiz, host.getId());
            room.getPlayers().put(host.getId(), host);
        } while (rooms.putIfAbsent(room.getCode(), room) != null);

        log.info("Utworzono pokój {} dla quizu {} (host: {})", room.getCode(), quiz.getTitle(), hostNick);
        return new CreateRoomResponse(room.getCode(), host.getId(), host.getToken());
    }

    // WS
    public void join(WebSocketSession session, ClientMessage.Join msg) {
        Room room = findRoom(msg.roomCode());
        if (room == null) {
            throw new RoomNotFoundException(msg.roomCode());
        }
        executeInRoomSafely(room, session, () -> {
            if (isBound(session)) {
                throw new OccupiedSessionException();
            }
            if (room.getState() != RoomState.LOBBY) {
                throw new InvalidRoomStateException(room.getState(), InvalidRoomStateExceptionReason.JOINING);
            }
            String nick = msg.nick() == null ? "" : msg.nick().trim();
            if (nick.isEmpty() || nick.length() > MAX_NICK_LENGTH) {
                throw new InvalidNickException(MAX_NICK_LENGTH);
            }
            boolean taken = room.getPlayers().values().stream()
                    .anyMatch(p -> p.getNick().equalsIgnoreCase(nick));
            if (taken) {
                throw new NickTakenException(nick);
            }
            if (room.getPlayers().size() >= MAX_PLAYERS) {
                throw new RoomFullException();
            }

            Player player = new Player(nick);
            player.setSession(session);
            room.getPlayers().put(player.getId(), player);
            bind(session, room, player);

            sender.send(session, new ServerMessage.Joined(player.getId(), player.getToken(), room.getCode()));
            broadcastLobby(room);
        });
    }

    public void rejoin(WebSocketSession session, ClientMessage.Rejoin msg) {
        Room room = findRoom(msg.roomCode());
        if (room == null) {
            throw new RoomNotFoundException(msg.roomCode());
        }
        executeInRoomSafely(room ,session, () -> {
            if (isBound(session)) {
                throw new OccupiedSessionException();
            }
            Player player = msg.playerId() == null ? null : room.getPlayers().get(msg.playerId());
            if (player == null || msg.token() == null || !player.getToken().equals(msg.token())) {
                throw new InvalidPlayerOrTokenRejoinException();
            }

            WebSocketSession old = player.getSession();
            player.setSession(session);
            player.setConnected(true);
            player.setDisconnectedAt(null);
            bind(session, room, player);
            if (old != null && old != session) {
                closeQuietly(old); // afterConnectionClosed will be called for the old session, therefore calling the disconnected
                // method below, but thanks to the check in disconnected() that it references the unused session by user,
                // it will not affect the player state.
            }

            sender.send(session, snapshot(room, player));
            broadcastLobby(room);
        });
    }

    public void disconnected(WebSocketSession session) {
        Object code = session.getAttributes().get(ATTR_ROOM);
        Object playerId = session.getAttributes().get(ATTR_PLAYER);
        if (code == null || playerId == null) return;

        Room room = rooms.get((String) code);
        if (room == null) return;

        executeInRoomSafely(room, session, () -> {
            Player player = room.getPlayers().get((String) playerId);
            if (player == null || player.getSession() != session) return;

            player.setSession(null);
            player.setConnected(false);
            player.setDisconnectedAt(System.currentTimeMillis());

            if (room.getState() == RoomState.LOBBY && !room.isHost(player)) {
                room.getPlayers().remove(player.getId());
            }
            broadcastLobby(room);
            // TODO: closing empty and inactive rooms after a certain time (TTL)
        });
    }

    // Helpers
    private ServerMessage snapshot(Room room, Player player) {
        // TODO: If the game is in progress, we should also include the current question and whether the player has already answered it.
        return new ServerMessage.RejoinSnapshot(room.getState(), null, false, player.getScore(), List.of());
    }

    private void broadcastLobby(Room room) {
        List<String> nicks = room.getPlayers().values().stream()
                .filter(p -> !room.isHost(p))
                .map(Player::getNick)
                .toList();
        ServerMessage msg = new ServerMessage.LobbyUpdate(nicks);
        room.getPlayers().values().forEach(p -> sender.send(p.getSession(), msg));
    }

    private Room findRoom(String code) {
        return code == null ? null : rooms.get(code.trim().toUpperCase());
    }

    private boolean isBound(WebSocketSession session) {
        return session.getAttributes().containsKey(ATTR_PLAYER);
    }

    private void bind(WebSocketSession session, Room room, Player player) {
        session.getAttributes().put(ATTR_ROOM, room.getCode());
        session.getAttributes().put(ATTR_PLAYER, player.getId());
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(ThreadLocalRandom.current().nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    // Have to catch exceptions manually inside the room.execute() as it won't propagate them to the caller.
    private void executeInRoomSafely(Room room, WebSocketSession session, MessageSender.Task task) {
        room.execute(()-> {
            sender.executeAndHandleErrors(session, task);
        });
    }
}
