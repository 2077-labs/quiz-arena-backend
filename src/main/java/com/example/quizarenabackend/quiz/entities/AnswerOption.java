package com.example.quizarenabackend.quiz.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AnswerOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String text;
    private boolean correct;
    private int position;
    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    @JsonIgnore
    private Question question;
}
