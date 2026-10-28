package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class NoPlayersException extends FriendlyException {
    public static final String CODE = "NO_PLAYERS";

    public NoPlayersException() {
        super(CODE, "Nie można rozpocząć gry bez żadnego gracza.");
    }
}
