package com.example.quizarenabackend.exceptions;

public class QuizNotFoundException extends FriendlyException {
    public static final String CODE = "QUIZ_NOT_FOUND";
    public QuizNotFoundException(Long quizId) {
        super(CODE, "Nie udało się znaleźć quizu o ID: " + quizId + ". Proszę sprawdzić ID quizu i spróbować ponownie.");
    }
}
