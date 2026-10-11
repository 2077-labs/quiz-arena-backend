package com.example.quizarenabackend.game;

import com.example.quizarenabackend.quiz.entities.Question;

public class ScoringService {
    public int calculateScore(Question question, long elapsedTime, boolean correct) {
        if (!correct) {
            return 0;
        }
        var scoreRatio = (double) elapsedTime / (question.getTimeLimitSec() * 1000L);
        int pts = (int) (question.getPoints() * (1-scoreRatio/2));
        return Math.max(pts, question.getPoints() / 2); // minimum half points for correct answer
    }
}
