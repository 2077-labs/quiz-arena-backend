package com.example.quizarenabackend.quiz.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundLeaderboardEntryDto {
    private String nick;
    private int score;
}
