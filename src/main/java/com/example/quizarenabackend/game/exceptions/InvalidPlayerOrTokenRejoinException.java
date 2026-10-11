package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class InvalidPlayerOrTokenRejoinException extends FriendlyException {
    public static final String CODE = "INVALID_PLAYER_OR_TOKEN_REJOIN";

    public InvalidPlayerOrTokenRejoinException() {
        super(CODE, "Nieprawidłowe dane do ponownego dołączenia do gry.");
    }
}
