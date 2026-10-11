package com.example.quizarenabackend.quiz.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String text;
    private int timeLimitSec = 20;
    private int points = 1000;
    private int position;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<AnswerOption> answerOptions = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name="quiz_id")
    private Quiz quiz;

    public void addAnswerOption(AnswerOption answerOption) {
        answerOptions.add(answerOption);
        answerOption.setQuestion(this);
    }
}
