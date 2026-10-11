package com.example.quizarenabackend.quiz.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDto {
    private long id;
    private int timeLimitSec;
    private int points;
    private int position;
    private String text;
    private List<AnswerOptionDto> options;
}
