package com.example.quizarenabackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDto {
    public static String INTERNAL_SERVER_ERROR_STATUS = "INTERNAL_SERVER_ERROR";
    public static String UNREADABLE_CLIENT_MESSAGE_STATUS = "MALFORMED_MESSAGE";
    public static String NOT_IMPLEMENTED_STATUS = "NOT_IMPLEMENTED";
    public static String VALIDATION_ERROR_STATUS = "VALIDATION_ERROR";
    public static String BAD_REQUEST_STATUS = "BAD_REQUEST";

    private String code;
    private String message;
    private Map<String, List<String>> validationErrors = new HashMap<>();

    public ErrorDto(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
