package com.example.quizarenabackend.game.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomResponse {
    private String roomCode;
    private String playerId; // id of the host
    private String token; // token of the host, required for rejoining
}