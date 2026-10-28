package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class QuestionMismatchException extends FriendlyException {
    public static final String CODE = "QUESTION_MISMATCH";

    public QuestionMismatchException() {
        super(CODE, "Ta odpowiedź dotyczy innego pytania niż aktualne.");
    }
}
