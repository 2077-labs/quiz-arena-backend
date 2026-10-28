package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class HostCannotAnswerException extends FriendlyException {
    public static final String CODE = "HOST_CANNOT_ANSWER";

    public HostCannotAnswerException() {
        super(CODE, "Prowadzący nie odpowiada na pytania.");
    }
}
