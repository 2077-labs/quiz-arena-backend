package com.example.quizarenabackend.quiz.dtos;

import lombok.Data;

@Data
public class QuizDto {
    private Long id;
    private String title;
    private String description;
    private int totalQuestions;
}
