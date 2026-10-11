package com.example.quizarenabackend.exceptions;

public class OccupiedSessionException extends FriendlyException {
    public static final String CODE = "OCCUPIED_SESSION";
    public OccupiedSessionException() {
        super(CODE, "Nie można dołączyć do pokoju, ponieważ sesja jest już zajęta. Proszę wylogować się z obecnej gry i spróbować ponownie.");
    }
}
