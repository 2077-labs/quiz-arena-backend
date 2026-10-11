package com.example.quizarenabackend.exceptions;

public class NoQuestionsQuizException extends FriendlyException {
    public static final String CODE = "NO_QUESTIONS_QUIZ";
    public NoQuestionsQuizException(String quizTitle) {
        super(CODE, "Quiz '" + quizTitle + "' nie zawiera żadnych pytań. Proszę dodać pytania do quizu i spróbować ponownie.");
    }
}
