package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class RoomFullException extends FriendlyException {
    public static final String CODE = "ROOM_FULL";
    public RoomFullException() {
        super(CODE, "Pokój jest pełny.");
    }
}
