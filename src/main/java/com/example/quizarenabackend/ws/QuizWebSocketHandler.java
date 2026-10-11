package com.example.quizarenabackend.ws;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.exceptions.FriendlyException;
import com.example.quizarenabackend.game.RoomService;
import com.example.quizarenabackend.ws.messages.ClientMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class QuizWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(QuizWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final RoomService roomService;
    private final MessageSender sender;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        sender.executeAndHandleErrors(session, () -> {
            ClientMessage payload = objectMapper.readValue(message.getPayload(), ClientMessage.class);

            switch (payload) {
                case ClientMessage.Join j -> roomService.join(session, j);
                case ClientMessage.Rejoin r -> roomService.rejoin(session, r);
                case ClientMessage.StartGame s -> notImplemented(session);
                case ClientMessage.SubmitAnswer a -> notImplemented(session);
                case ClientMessage.NextQuestion n -> notImplemented(session);
            }
        });
    }

    private void notImplemented(WebSocketSession session) {
        sender.sendError(session, ErrorDto.NOT_IMPLEMENTED_STATUS, "Ta funkcjonalność nie została jeszcze zaimplementowana.");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        roomService.disconnected(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Błąd w komunikacji WebSocket dla sesji {}: {}", session.getId(), exception.getMessage());
        sender.sendError(session, ErrorDto.INTERNAL_SERVER_ERROR_STATUS, "Wystąpił błąd w komunikacji.");
    }
}
