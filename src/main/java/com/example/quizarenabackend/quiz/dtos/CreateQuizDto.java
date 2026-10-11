package com.example.quizarenabackend.quiz.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateQuizDto {
    @NotBlank(message = "Podaj tytuł quizu")
    private String title;
    @NotBlank(message = "Podaj opis quizu")
    private String description;
    @Valid
    List<@Valid CreateQuestionDto> questions;
}
