package com.example.quizarenabackend.game.dtos;

import com.example.quizarenabackend.quiz.dtos.AnswerOptionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentQuestionDto {
    private long questionId;
    private int index;
    private int totalQuestions;
    private String text;
    private List<GameQuestionOptionDto> options;
    private long endsAt;
    private long serverNow;
}
