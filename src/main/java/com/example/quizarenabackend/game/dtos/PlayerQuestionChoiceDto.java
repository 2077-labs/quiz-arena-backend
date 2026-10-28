package com.example.quizarenabackend.game.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerQuestionChoiceDto {
    private String id;
    private String nick;
}
