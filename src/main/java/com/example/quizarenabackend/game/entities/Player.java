package com.example.quizarenabackend.game.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Player {
    @EqualsAndHashCode.Include
    private final String id;
    @ToString.Exclude
    private final String token;

    private String nick;
    private int score = 0;
    private boolean connected = true;
    private Long disconnectedAt;
    @ToString.Exclude
    private WebSocketSession session;

    public Player(String nick) {
        this.id = UUID.randomUUID().toString();
        this.token = UUID.randomUUID().toString();
        this.nick = nick;
    }
}
