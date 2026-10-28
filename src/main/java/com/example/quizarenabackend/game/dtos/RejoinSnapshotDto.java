package com.example.quizarenabackend.game.dtos;

import com.example.quizarenabackend.game.entities.RoomState;
import com.example.quizarenabackend.quiz.dtos.RoundLeaderboardEntryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejoinSnapshotDto {
    private RoomState roomState;
    private CurrentQuestionDto question;
    private boolean alreadyAnswered;
    private int myScore;
    private List<RoundLeaderboardEntryDto> leaderboard;
    private RoundResultDto lastResult;
    private boolean hostConnected;
}
