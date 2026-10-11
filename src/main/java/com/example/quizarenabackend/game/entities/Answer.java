package com.example.quizarenabackend.game.entities;

public record Answer(long optionId, long receivedAt, boolean correct, int points) {}