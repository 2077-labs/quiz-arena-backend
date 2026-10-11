package com.example.quizarenabackend.quiz.exceptons;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class SameTitleQuizExistsException extends FriendlyException {
    public final static String CODE = "SAME_TITLE_QUIZ_EXISTS";

    public SameTitleQuizExistsException(String title) {
        super(CODE, "Istnieje już quiz o tytule: " + title);
    }
}
