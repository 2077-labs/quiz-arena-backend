package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;
import com.example.quizarenabackend.game.entities.RoomState;

public class InvalidRoomStateException extends FriendlyException {
    public static final String CODE = "INVALID_ROOM_STATE";

    public InvalidRoomStateException(RoomState roomState, InvalidRoomStateExceptionReason reason) {
        String msg = null;
        switch (reason) {
            case JOINING -> {
                msg = switch (roomState) {
                    case LOBBY -> "Nie można dołączyć do pokoju, ponieważ jest w trybie poczekalni.";
                    case QUESTION -> "Nie można dołączyć do pokoju, ponieważ rozgrywka już trwa.";
                    case RESULT -> "Nie można dołączyć do pokoju, ponieważ trwa wyświetlanie wyników.";
                    case FINISHED -> "Nie można dołączyć do pokoju, ponieważ gra została zakończona.";
                };
            }
        }
        super(CODE, msg);
    }
}
