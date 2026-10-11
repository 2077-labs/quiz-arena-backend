package com.example.quizarenabackend.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FriendlyException extends RuntimeException {
    private final String code;
    private final HttpStatus requestedStatus;

    public FriendlyException(String code, String message) {
        super(message);
        this.code = code;
        this.requestedStatus = null;
    }
    public FriendlyException(String code, String message, HttpStatus requestedStatus) {
        super(message);
        this.code = code;
        this.requestedStatus = requestedStatus;
    }
}
