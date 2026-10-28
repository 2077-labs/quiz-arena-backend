package com.example.quizarenabackend.ws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix="quizarena.ws")
@Data
public class QuizArenaWsProperties {
    private Duration answerGrace = Duration.ofMillis(500);
    private Duration hostGrace = Duration.ofSeconds(60);
    private Duration hostFirstConnect = Duration.ofMinutes(2);
    private Duration roomIdleTimeout = Duration.ofMinutes(30);
    private Duration finishedRoomTtl = Duration.ofMinutes(10);
    private Duration inactiveRoomSweepInterval = Duration.ofMinutes(10);
    private Duration heartbeatTimeout = Duration.ofSeconds(30);
    private Duration heartbeatInterval = Duration.ofSeconds(10);
}
