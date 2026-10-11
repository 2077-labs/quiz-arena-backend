package com.example.quizarenabackend.quiz.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateQuestionAnswerDto {
    @NotBlank(message = "Podaj treść odpowiedzi")
    private String text;
    private Boolean correct;

    private Long questionId;
}
