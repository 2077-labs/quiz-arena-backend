package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class NotInRoomException extends FriendlyException {
    public static final String CODE = "NOT_IN_ROOM";

    public NotInRoomException() {
        super(CODE, "Nie jesteś w żadnym pokoju. Najpierw dołącz do gry.");
    }
}
