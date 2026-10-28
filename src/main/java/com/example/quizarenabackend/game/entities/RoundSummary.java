package com.example.quizarenabackend.game.entities;

import com.example.quizarenabackend.game.dtos.PlayerQuestionChoiceDto;
import com.example.quizarenabackend.quiz.dtos.RoundLeaderboardEntryDto;

import java.util.List;
import java.util.Map;

// This class is used to calculate the shared round summary for all players in a game in order to then distribute it
// joined with the individual round result to each player in the game via RoundResultDto.
public record RoundSummary(long questionId,
                           List<Long> correctOptionIds,
                           Map<Long, List<PlayerQuestionChoiceDto>> distribution,
                           List<RoundLeaderboardEntryDto> leaderboard) {}