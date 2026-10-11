package com.example.quizarenabackend.quiz.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class GameResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String roomCode;
    @ManyToOne(optional = false)
    @JoinColumn(name="quiz_id")
    private Quiz quiz;
    @NotNull
    private Instant playedAt;
    @NotBlank
    private String nick;
    private int score;
}