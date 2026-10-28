package com.example.quizarenabackend.game;

import com.example.quizarenabackend.game.dtos.CurrentQuestionDto;
import com.example.quizarenabackend.game.dtos.PlayerQuestionChoiceDto;
import com.example.quizarenabackend.game.dtos.RejoinSnapshotDto;
import com.example.quizarenabackend.game.dtos.RoundResultDto;
import com.example.quizarenabackend.game.entities.Player;
import com.example.quizarenabackend.game.entities.Room;
import com.example.quizarenabackend.game.entities.Round;
import com.example.quizarenabackend.game.entities.RoundSummary;
import com.example.quizarenabackend.game.mappers.PlayerMapper;
import com.example.quizarenabackend.quiz.QuizMapper;
import com.example.quizarenabackend.quiz.dtos.RoundLeaderboardEntryDto;
import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.Question;
import com.example.quizarenabackend.ws.messages.ServerMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GameMessageFactory {
    private static final Logger log = LoggerFactory.getLogger(GameMessageFactory.class);
    private final PlayerMapper playerMapper;
    private final QuizMapper quizMapper;

    public List<Player> participants(Room room) {
        return room.getPlayers().values().stream().toList();
    }
    public List<RoundLeaderboardEntryDto> leaderboard(Room room) {
        return participants(room)
                .stream()
                .sorted(Comparator.comparingInt(Player::getScore).reversed())
                .map(playerMapper::toRoundLeaderboardEntryDto)
                .toList();
    }
    public CurrentQuestionDto question(Room room, Round round, long now) {
        Question question = round.getQuestion();
        return quizMapper.toCurrentQuestionDto(
                question,
                room.getQuestionIndex(),
                room.getQuiz().getQuestions().size(),
                round.getEndsAt(),
                now
        );
    }
    public RoundSummary summarize(Room room, Round round) {
        Question question = round.getQuestion();
        List<Long> correctOptionIds = question.getAnswerOptions().stream()
                .filter(AnswerOption::isCorrect)
                .map(AnswerOption::getId)
                .toList();
        var playerAnswerMap = round.getAnswers();
        var players = participants(room);
        var playersById = players.stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        var answerIdToPlayerQuestionChoiceMap = new HashMap<Long, List<PlayerQuestionChoiceDto>>();
        for (var entry : playerAnswerMap.entrySet()) {
            var playerId = entry.getKey();
            var relatedPlayer = playersById.get(playerId);
            // This should never happen, but just in case we omit the player from the distribution if they are not in the room.
            if (relatedPlayer == null) {
                log.error(
                        "This is very unexpected: playerId {} answered but is not in the room",
                        playerId
                );
                continue;
            }
            var answerOptionId = entry.getValue().optionId();
            answerIdToPlayerQuestionChoiceMap
                    .computeIfAbsent(answerOptionId, key -> new ArrayList<>())
                    .add(playerMapper.toPlayerQuestionChoiceDto(relatedPlayer));
        }

        return new RoundSummary(
                question.getId(),
                correctOptionIds,
                answerIdToPlayerQuestionChoiceMap,
                leaderboard(room)
        );
    }
    public RoundResultDto resultFor(RoundSummary summary, Round round, Player player) {
        var playerAnswer = round.getAnswers().get(player.getId());
        return new RoundResultDto(
                summary.questionId(),
                summary.correctOptionIds(),
                summary.distribution(),
                summary.leaderboard(),
                playerAnswer == null ? null : playerAnswer.correct(),
                playerAnswer == null ? 0 : playerAnswer.points()
        );
    }
    public RejoinSnapshotDto snapshot(Room room, Player player) {
        CurrentQuestionDto question = null;
        RoundResultDto lastResult = null;
        boolean alreadyAnswered = false;

        switch (room.getState()) {
            case QUESTION -> {
                question = question(room, room.getRound(), System.currentTimeMillis());
                alreadyAnswered = room.getRound().getAnswers().containsKey(player.getId());
            }
            case RESULT -> {
                // Here we utilize the fact that the round summary has already been computed and stored in the round
                // object, so we don't need to recompute it.
                lastResult = resultFor(room.getRound().getRoundSummary(), room.getRound(), player);
            }
            case LOBBY, FINISHED -> {
            }
        }

        List<RoundLeaderboardEntryDto> leaderboard = switch (room.getState()) {
            case LOBBY -> List.of();
            default -> leaderboard(room);
        };

        Player host = room.getPlayers().get(room.getHostId());
        boolean hostConnected = host != null && host.isConnected();

        return new RejoinSnapshotDto(
                room.getState(),
                question,
                alreadyAnswered,
                player.getScore(),
                leaderboard,
                lastResult,
                hostConnected
        );
    }
}
