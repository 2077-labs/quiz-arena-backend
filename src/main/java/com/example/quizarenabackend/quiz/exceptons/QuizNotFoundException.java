package com.example.quizarenabackend.quiz.exceptons;

import com.example.quizarenabackend.exceptions.FriendlyException;
import org.springframework.http.HttpStatus;

public class QuizNotFoundException extends FriendlyException {
    public static final String CODE = "QUIZ_NOT_FOUND";

    public QuizNotFoundException(Long id, boolean dueToPathVariable) {
        super(CODE, "Nie znaleziono quizu o id: " + id, dueToPathVariable ? HttpStatus.NOT_FOUND : null);
    }
    public QuizNotFoundException(Long id) {
        this(id, false);
    }
}
