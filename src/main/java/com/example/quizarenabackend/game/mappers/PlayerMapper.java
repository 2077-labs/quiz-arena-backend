package com.example.quizarenabackend.game.mappers;

import com.example.quizarenabackend.game.dtos.PlayerQuestionChoiceDto;
import com.example.quizarenabackend.game.entities.Player;
import com.example.quizarenabackend.quiz.dtos.RoundLeaderboardEntryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlayerMapper {
    RoundLeaderboardEntryDto toRoundLeaderboardEntryDto(Player player);
    PlayerQuestionChoiceDto toPlayerQuestionChoiceDto(Player player);
}
