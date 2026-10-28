package com.example.quizarenabackend.game;

import com.example.quizarenabackend.quiz.entities.Question;
import org.springframework.stereotype.Service;

@Service
public class ScoringService {
    public int calculateScore(Question question, long elapsedTime, boolean correct) {
        if (!correct) {
            return 0;
        }
        long limitMs = question.getTimeLimitSec() * 1000L;
        long elapsed = Math.max(0, Math.min(elapsedTime, limitMs));
        double ratio = limitMs == 0 ? 1.0 : (double) elapsed / limitMs;
        int pts = (int) (question.getPoints() * (1-ratio/2));
        return Math.max(pts, question.getPoints() / 2); // minimum half points for correct answer
    }
}
