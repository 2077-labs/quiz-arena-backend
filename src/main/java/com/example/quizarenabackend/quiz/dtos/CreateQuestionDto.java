package com.example.quizarenabackend.quiz.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateQuestionDto {
    @NotBlank(message = "Podaj treść pytania")
    private String text;
    @Valid()
    private List<@Valid CreateQuestionAnswerDto> options = new ArrayList<>();

    private Long quizId;
}
