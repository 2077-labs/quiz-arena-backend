package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class InvalidOptionException extends FriendlyException {
    public static final String CODE = "INVALID_OPTION";

    public InvalidOptionException() {
        super(CODE, "Wybrana odpowiedź nie istnieje w tym pytaniu.");
    }
}
