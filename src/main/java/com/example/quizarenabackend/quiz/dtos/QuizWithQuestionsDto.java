package com.example.quizarenabackend.quiz.dtos;

import lombok.Data;

import java.util.List;

@Data
public class QuizWithQuestionsDto {
    private Long id;
    private String title;
    private String description;
    private List<QuestionDto> questions;
}
