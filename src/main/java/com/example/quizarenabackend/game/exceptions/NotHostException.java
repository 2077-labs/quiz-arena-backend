package com.example.quizarenabackend.game.exceptions;

import com.example.quizarenabackend.exceptions.FriendlyException;

public class NotHostException extends FriendlyException {
    public static final String CODE = "NOT_HOST";

    public NotHostException() {
        super(CODE, "Tę akcję może wykonać tylko prowadzący grę.");
    }
}
