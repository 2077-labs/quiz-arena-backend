package com.example.quizarenabackend.exceptions;

public class NickTakenException extends FriendlyException {
    public static final String CODE = "NICK_TAKEN";
    public NickTakenException(String nick) {
        super(CODE, "Nazwa gracza '" + nick + "' jest już zajęta w tej grze.");
    }
}
