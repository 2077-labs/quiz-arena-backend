package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;
import com.example.quizarenabackend.game.entities.RoomState;

public class InvalidRoomStateException extends FriendlyException {
    public static final String CODE = "INVALID_ROOM_STATE";

    public InvalidRoomStateException(RoomState roomState, InvalidRoomStateExceptionReason reason) {
        super(CODE, buildMessage(roomState, reason));
    }

    private static String buildMessage(RoomState roomState, InvalidRoomStateExceptionReason reason) {
        return switch (reason) {
            case JOINING -> switch (roomState) {
                case LOBBY -> "Nie można dołączyć do pokoju, ponieważ jest w trybie poczekalni.";
                case QUESTION -> "Nie można dołączyć do pokoju, ponieważ rozgrywka już trwa.";
                case RESULT -> "Nie można dołączyć do pokoju, ponieważ trwa wyświetlanie wyników.";
                case FINISHED -> "Nie można dołączyć do pokoju, ponieważ gra została zakończona.";
            };
            case STARTING -> "Grę można rozpocząć tylko z poczekalni.";
            case ANSWERING -> switch (roomState) {
                case LOBBY -> "Gra jeszcze się nie rozpoczęła.";
                case QUESTION -> "Nie można teraz odpowiedzieć na pytanie.";
                case RESULT -> "Czas na odpowiedź już minął.";
                case FINISHED -> "Gra została zakończona.";
            };
            case ADVANCING -> "Do następnego pytania można przejść dopiero po wyświetleniu wyników rundy.";
        };
    }
}
