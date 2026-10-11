package com.example.quizarenabackend.exceptions;

public class RoomNotFoundException extends FriendlyException {
    private final static String CODE = "ROOM_NOT_FOUND";
    public RoomNotFoundException() {
        super(CODE, "Nie udało się znaleźć pokoju. Proszę sprawdzić kod pokoju i spróbować ponownie.");
    }
    public RoomNotFoundException(String roomCode) {
        super(CODE, "Nie udało się znaleźć pokoju o kodzie: " + roomCode + ". Proszę sprawdzić kod pokoju i spróbować ponownie.");
    }
}
