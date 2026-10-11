package com.example.quizarenabackend.ws;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.exceptions.FriendlyException;
import com.example.quizarenabackend.ws.messages.ClientMessage;
import com.example.quizarenabackend.ws.messages.ServerMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import java.io.IOException;

@Slf4j
@Component
public class MessageSender {
    private final ObjectWriter writer;

    public MessageSender(ObjectMapper objectMapper) {
        // writerFor method guarantees that the serialization will be done according to the ServerMessage class structure
        // (with the help of the type field in the JSON)
        this.writer = objectMapper.writerFor(ServerMessage.class);
    }

    public void send(WebSocketSession session, ServerMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = writer.writeValueAsString(message);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException | IllegalStateException | JacksonException e) {
            log.warn("Nie udało się wysłać wiadomości do sesji {}: {}", session.getId(), e.getMessage());
        }
    }

    public void sendError(WebSocketSession session, String code, String message) {
        ServerMessage.ErrorMsg errorMsg = new ServerMessage.ErrorMsg(new ErrorDto(code, message));
        send(session, errorMsg);
    }

    @FunctionalInterface
    public interface Task {
        void run() throws Exception;
    }

    public void executeAndHandleErrors(WebSocketSession session, Task task) {
        try {
            task.run();
        } catch (FriendlyException e) {
            log.error("Błąd przy obsłudze wiadomości od klienta: {}, rodzaj {}", e.getMessage(), e.getClass().getSimpleName());
            sendError(session, e.getCode(), e.getMessage());
        } catch (JacksonException e) {
            log.error("Nieprawidłowy format JSON lub nieznany typ wiadomości: {}", e.getMessage());
            sendError(session, ErrorDto.UNREADABLE_CLIENT_MESSAGE_STATUS, "Nieprawidłowy format JSON lub nieznany typ wiadomości");
        } catch (Exception e) {
            log.error("Nieoczekiwany błąd przy obsłudze wiadomości od klienta: {}", e.getMessage(), e);
            sendError(session, ErrorDto.INTERNAL_SERVER_ERROR_STATUS, "Wystąpił nieoczekiwany błąd po stronie serwera.");
        }
    }
}
