package com.example.quizarenabackend.exceptions;

public class InvalidNickException extends FriendlyException {
    public static final String CODE = "INVALID_NICK";

    public InvalidNickException(int maxLength) {
        super(CODE, "Nick musi mieć od 1 do " + maxLength + " znaków");
    }
}
