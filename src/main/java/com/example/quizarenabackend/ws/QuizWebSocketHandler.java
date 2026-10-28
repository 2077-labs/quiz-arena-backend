package com.example.quizarenabackend.ws;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.exceptions.FriendlyException;
import com.example.quizarenabackend.game.RoomService;
import com.example.quizarenabackend.ws.messages.ClientMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class QuizWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(QuizWebSocketHandler.class);
    private static final String ATTR_LAST_PONG = "lastPongAt";

    private final ObjectMapper objectMapper;
    private final RoomService roomService;
    private final MessageSender sender;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final QuizArenaWsProperties properties;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        session.getAttributes().put(ATTR_LAST_PONG, System.currentTimeMillis());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        sender.executeAndHandleErrors(session, () -> {
            ClientMessage payload = objectMapper.readValue(message.getPayload(), ClientMessage.class);

            switch (payload) {
                case ClientMessage.Join j -> roomService.join(session, j);
                case ClientMessage.Rejoin r -> roomService.rejoin(session, r);
                case ClientMessage.StartGame s -> roomService.startGame(session);
                case ClientMessage.SubmitAnswer a -> roomService.submitAnswer(session, a);
                case ClientMessage.NextQuestion n -> roomService.nextQuestion(session);
            }
        });
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        session.getAttributes().put(ATTR_LAST_PONG, System.currentTimeMillis());
    }

    @Scheduled(fixedDelayString = "#{quizArenaWsProperties.heartbeatInterval}")
    public void heartbeat() {
        long now = System.currentTimeMillis();
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            Object value = session.getAttributes().get(ATTR_LAST_PONG);
            long lastPong = value instanceof Long l ? l : now;
            if (now - lastPong > properties.getHeartbeatTimeout().toMillis()) {
                log.info("Zamykam sesję {} z powodu braku odpowiedzi na ping.", session.getId());
                sessions.remove(session);
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ignored) {
                }
                // Dead connections may not deliver afterConnectionClosed, therefore we call disconnected here to ensure
                // the room is cleaned up.
                roomService.disconnected(session);
            }
            try {
                synchronized (session) {
                    session.sendMessage(new PongMessage());
                }
            } catch (IOException | IllegalStateException e) {
                log.error("Nie udało się wysłać wiadomości ping do sesji {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        roomService.disconnected(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Błąd w komunikacji WebSocket dla sesji {}: {}", session.getId(), exception.getMessage());
        sender.sendError(session, ErrorDto.INTERNAL_SERVER_ERROR_STATUS, "Wystąpił błąd w komunikacji.");
    }
}
