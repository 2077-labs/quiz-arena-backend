package com.example.quizarenabackend;

import com.example.quizarenabackend.exceptions.FriendlyException;
import com.example.quizarenabackend.dtos.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(FriendlyException.class)
    public ResponseEntity<ErrorDto> handleFriendlyException(FriendlyException ex) {
        ErrorDto errorDto = new ErrorDto(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getRequestedStatus()!=null ? ex.getRequestedStatus() : HttpStatus.BAD_REQUEST).body(errorDto);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidationException(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        Map<String, List<String>> validationErrors = fieldErrors.stream()
                .collect(
                        groupingBy(
                                FieldError::getField,
                                mapping(FieldError::getDefaultMessage, toList())
                        )
                );
        return new ResponseEntity<>(new ErrorDto(ErrorDto.VALIDATION_ERROR_STATUS, "Walidacja nie powiodła się", validationErrors), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrorDto errorDto = new ErrorDto(ErrorDto.BAD_REQUEST_STATUS, "Niepoprawny format danych wejściowych.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleException(Exception ex) {
        log.error("Wystąpił nieoczekiwany błąd (REST): ", ex);
        ErrorDto errorDto = new ErrorDto(ErrorDto.INTERNAL_SERVER_ERROR_STATUS, "Wystąpił nieoczekiwany błąd.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }
}
