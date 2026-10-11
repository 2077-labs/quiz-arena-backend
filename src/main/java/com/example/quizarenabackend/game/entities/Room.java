package com.example.quizarenabackend.game.entities;

import com.example.quizarenabackend.quiz.entities.Quiz;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Data
public class Room {
    private final String code;
    private final Quiz quiz;
    private final String hostId;
    private final String roomSessionId = UUID.randomUUID().toString();
    private RoomState state = RoomState.LOBBY;
    private final Map<String, Player> players = new LinkedHashMap<>();
    private Round round;
    private int questionIndex = -1;

    // All the tasks have to be executed in the same thread, otherwise we could get race conditions
    // related to the state of the room.
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    public Room(String code, Quiz quiz, String hostId) {
        this.code = code;
        this.quiz = quiz;
        this.hostId = hostId;
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }
    public void close() {
        executor.shutdownNow();
    }
    public boolean hasNextQuestion() {
        return questionIndex + 1 < quiz.getQuestions().size();
    }
    public boolean isHost(Player player) {
        return hostId.equals(player.getId());
    }
}
