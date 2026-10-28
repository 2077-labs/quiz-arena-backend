package com.example.quizarenabackend.game.entities;

import com.example.quizarenabackend.quiz.entities.Quiz;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@Data
@Getter
@Slf4j
public class Room {
    private final String code;
    private final Quiz quiz;
    private final String hostId;
    private final Map<String, Player> players = new LinkedHashMap<>();

    @Setter private RoomState state = RoomState.LOBBY;
    @Setter private Round round;
    @Setter private int questionIndex = -1;
    @Setter private ScheduledFuture<?> roundTimer;
    @Setter private ScheduledFuture<?> hostDisconnectTimer;
    private long lastActivityAt = System.currentTimeMillis();
    private boolean closed;

    // All the tasks have to be executed in the same thread, otherwise we could get race conditions
    // related to the state of the room (for example the round timer and a player's answer).
    @Getter(AccessLevel.NONE)
    private final ScheduledThreadPoolExecutor executor;

    public Room(String code, Quiz quiz, String hostId) {
        this.code = code;
        this.quiz = quiz;
        this.hostId = hostId;

        ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(1,
                runnable -> {
                    Thread thread = new Thread(runnable, "room-" + code);
                    thread.setDaemon(true);
                    return thread;
                });
        ex.setRemoveOnCancelPolicy(true);
        ex.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.executor = ex;
    }

    // Runs the task on the room thread.
    public void execute(Runnable task) {
        try {
            executor.execute(()->runSafely(task));
        } catch (RejectedExecutionException e) {
            log.debug("Pokój {} został zamknięty, nie można wykonać zadania.", code);
        }
    }
    // Runs the task on the room thread after the specified delay. If the room have been closed, null is returned.
    public ScheduledFuture<?> schedule(Runnable task, long delayMs) {
        try {
            return executor.schedule(()->runSafely(task), Math.max(0, delayMs), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null;
        }
    }
    public static void cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
    }
    public void close() {
        executor.shutdown();
    }
    public void touch() {
        lastActivityAt = System.currentTimeMillis();
    }
    public void markClosed() {
        closed = true;
    }
    public boolean hasNextQuestion() {
        return questionIndex + 1 < quiz.getQuestions().size();
    }
    public boolean isHost(Player player) {
        return hostId.equals(player.getId());
    }
    public List<WebSocketSession> getRelatedSessions() {
        return players.values().stream()
                .map(Player::getSession)
                .filter(Objects::nonNull)
                .toList();
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("Błąd w zadaniu pokoju {}", code, e);
        }
    }
}
