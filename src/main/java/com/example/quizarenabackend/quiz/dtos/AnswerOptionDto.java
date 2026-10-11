package com.example.quizarenabackend.quiz.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerOptionDto {
    private long id;
    private String text;
    private int position;
    private boolean correct;
}
