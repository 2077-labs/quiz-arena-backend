package com.example.quizarenabackend.exceptions;

public class InvalidPlayerOrTokenRejoinException extends FriendlyException {
    public static final String CODE = "INVALID_PLAYER_OR_TOKEN_REJOIN";

    public InvalidPlayerOrTokenRejoinException() {
        super(CODE, "Nieprawidłowe dane do ponownego dołączenia do gry.");
    }
}
