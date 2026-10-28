package com.example.quizarenabackend.game.entities;

import com.example.quizarenabackend.game.entities.Answer;
import com.example.quizarenabackend.quiz.entities.Question;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Round {
    private final Question question;
    private final long startedAt;   // server time in milliseconds
    private final long endsAt;
    private final Map<String, Answer> answers = new HashMap<>();

    // null until the round is finished, then it will be set to the summary of the round shared between all players in the game
    private RoundSummary roundSummary;

    public Round(Question question, long startedAt) {
        this.question = question;
        this.startedAt = startedAt;
        this.endsAt = startedAt + question.getTimeLimitSec() * 1000L;
    }

    public boolean submit(String playerId, Answer a) {
        return answers.putIfAbsent(playerId, a) == null;
    }
}