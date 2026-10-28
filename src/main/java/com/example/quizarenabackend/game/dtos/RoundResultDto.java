package com.example.quizarenabackend.game.dtos;

import com.example.quizarenabackend.quiz.dtos.RoundLeaderboardEntryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoundResultDto {
    private long questionId;
    private List<Long> correctOptionIds;
    // key: optionId, value: List<PlayerQuestionChoiceDto>
    private Map<Long, List<PlayerQuestionChoiceDto>> distribution;
    private List<RoundLeaderboardEntryDto> leaderboard;
    // null -> player did not answer
    private Boolean answeredCorrectly;
    // points earned by the player receiving this DTO in the current round
    private int pointsEarned;
}