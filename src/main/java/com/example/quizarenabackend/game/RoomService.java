package com.example.quizarenabackend.game;

import com.example.quizarenabackend.game.dtos.CreateRoomResponse;
import com.example.quizarenabackend.game.entities.CloseReason;
import com.example.quizarenabackend.game.entities.Player;
import com.example.quizarenabackend.game.entities.Room;
import com.example.quizarenabackend.game.entities.RoomState;
import com.example.quizarenabackend.game.exceptions.*;
import com.example.quizarenabackend.quiz.QuizRepository;
import com.example.quizarenabackend.quiz.entities.Quiz;
import com.example.quizarenabackend.quiz.exceptons.NoQuestionsQuizException;
import com.example.quizarenabackend.quiz.exceptons.QuizNotFoundException;
import com.example.quizarenabackend.ws.MessageSender;
import com.example.quizarenabackend.ws.QuizArenaWsProperties;
import com.example.quizarenabackend.ws.messages.ClientMessage;
import com.example.quizarenabackend.ws.messages.ServerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
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
    private final GameService gameService;
    private final GameMessageFactory messages;
    private final QuizArenaWsProperties properties;
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

        var created = room;
        room.execute(()->scheduleHostTimeout(created, properties.getHostFirstConnect().toMillis()));

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
                closeQuietly(old, CloseStatus.NORMAL.withReason("REPLACED")); // afterConnectionClosed will be called for the old session, therefore calling the disconnected
                // method below, but thanks to the check in disconnected() that it references the unused session by user,
                // it will not affect the player state.
            }

            if (room.isHost(player)) {
                // When the host reconnects, we cancel the host disconnect timer, because the host is now connected again.
                Room.cancel(room.getHostDisconnectTimer());
                room.setHostDisconnectTimer(null);
                broadcastToParticipants(room, new ServerMessage.HostStatus(true, null));
            }

            sender.send(session, new ServerMessage.RejoinSnapshot(messages.snapshot(room, player)));
            if (room.getState() == RoomState.LOBBY) {
                broadcastLobby(room);
            }
        });
    }

    private void broadcastToParticipants(Room room, ServerMessage message) {
        messages.participants(room).forEach(p -> sender.send(p.getSession(), message));
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

            if (room.isHost(player)) {
                onHostDisconnected(room);
            } else if (room.getState() == RoomState.LOBBY) {
                // If the game is not in progress, we remove the player from the room and notify the other participants.
                room.getPlayers().remove(player.getId());
                broadcastLobby(room);
            } else {
                // If the game is in progress, we don't remove the player from the room, but we notify the
                // game service that a participant has disconnected, allowing the player to rejoin later if they wish.
                gameService.onParticipantDisconnected(room);
            }
        });
    }

    private void onHostDisconnected(Room room) {
        if (room.getState() == RoomState.FINISHED) {
            // Here the finished room is going to be removed by the TTL
            return;
        }
        var hostGraceMs = properties.getHostGrace().toMillis();
        scheduleHostTimeout(room, hostGraceMs);
        long closesAt = System.currentTimeMillis() + hostGraceMs;
        broadcastToParticipants(room, new ServerMessage.HostStatus(false, closesAt));
        log.info("Host opuścił pokój {}, zamknięcie za {} ms", room.getCode(), hostGraceMs);
    }

    private void scheduleHostTimeout(Room room, long millis) {
        Room.cancel(room.getHostDisconnectTimer());
        room.setHostDisconnectTimer(room.schedule(()-> {
            Player host = room.getPlayers().get(room.getHostId());
            if (host != null && !host.isConnected()) {
                closeRoom(room, CloseReason.HOST_LEFT);
            }
        }, millis));
    }

    private void closeRoom(Room room, CloseReason reason) {
        if (room.isClosed()) return;
        room.markClosed();
        rooms.remove(room.getCode(), room);
        Room.cancel(room.getRoundTimer());
        Room.cancel(room.getHostDisconnectTimer());

        List<WebSocketSession> sessions = room.getRelatedSessions();
        ServerMessage closed = new ServerMessage.RoomClosed(reason);
        sessions.forEach(s->sender.send(s, closed));
        sessions.forEach(s->closeQuietly(s, CloseStatus.NORMAL.withReason(reason.toString())));

        room.close();
        log.info("Zamknięto pokój {} (powód: {})", room.getCode(), reason);
    }

    @Scheduled(fixedDelayString = "#{quizArenaWsProperties.inactiveRoomSweepInterval.toMillis()}")
    public void sweepIdleRooms() {
        for (Room room : rooms.values()) {
            room.execute(()-> {
                long limit = room.getState() == RoomState.FINISHED ? properties.getFinishedRoomTtl().toMillis() :
                        properties.getRoomIdleTimeout().toMillis();
                if (!room.isClosed() && System.currentTimeMillis() - room.getLastActivityAt() > limit) {
                    closeRoom(room, CloseReason.IDLE);
                }
            });
        }
    }

    // WS: active player game actions
    public void startGame(WebSocketSession session) {
        Room room = roomOf(session);
        executeInRoomSafely(room, session, () -> gameService.startGame(room, requirePlayer(room,session)));
    }

    public void submitAnswer(WebSocketSession session, ClientMessage.SubmitAnswer msg) {
        Room room = roomOf(session);
        executeInRoomSafely(room, session, () -> gameService.submitAnswer(room, requirePlayer(room,session), msg));
    }

    public void nextQuestion(WebSocketSession session) {
        Room room = roomOf(session);
        executeInRoomSafely(room, session, () -> gameService.nextQuestion(room, requirePlayer(room,session)));
    }

    // Helpers
    private Room roomOf(WebSocketSession session) {
        Object code = session.getAttributes().get(ATTR_ROOM);
        Room room = code == null ? null : rooms.get((String) code);
        if (room == null) {
            throw new RoomNotFoundException((String) code);
        }
        return room;
    }

    // Tries to retrieve the current player reference based on the session data.
    private Player requirePlayer(Room room, WebSocketSession session) {
        Object id = session.getAttributes().get(ATTR_PLAYER);
        Player player = id == null ? null : room.getPlayers().get((String) id);
        if (player == null || player.getSession() != session) {
            throw new NotInRoomException();
        }
        return player;
    }

    private void broadcastLobby(Room room) {
        List<String> nicks = room.getPlayers().values().stream()
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

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
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
            room.touch();
            sender.executeAndHandleErrors(session, task);
        });
    }
}
