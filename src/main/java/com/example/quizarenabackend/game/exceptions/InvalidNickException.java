package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class InvalidNickException extends FriendlyException {
    public static final String CODE = "INVALID_NICK";

    public InvalidNickException(int maxLength) {
        super(CODE, "Nick musi mieć od 1 do " + maxLength + " znaków");
    }
}
