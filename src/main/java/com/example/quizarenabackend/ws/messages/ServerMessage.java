package com.example.quizarenabackend.ws.messages;

import com.example.quizarenabackend.game.entities.RoomState;
import com.example.quizarenabackend.quiz.dtos.RoundLeaderboardEntryDto;
import com.example.quizarenabackend.quiz.dtos.QuestionDto;
import com.example.quizarenabackend.dtos.ErrorDto;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ServerMessage.Joined.class, name = "joined"),
        @JsonSubTypes.Type(value = ServerMessage.LobbyUpdate.class, name = "lobby_update"),
        @JsonSubTypes.Type(value = ServerMessage.QuestionMsg.class, name = "question"),
        @JsonSubTypes.Type(value = ServerMessage.AnswerAck.class, name = "answer_ack"),
        @JsonSubTypes.Type(value = ServerMessage.RoundResult.class, name = "round_result"),
        @JsonSubTypes.Type(value = ServerMessage.GameOver.class, name = "game_over"),
        @JsonSubTypes.Type(value = ServerMessage.RejoinSnapshot.class, name = "rejoin_snapshot"),
        @JsonSubTypes.Type(value = ServerMessage.ErrorMsg.class, name = "error")
})
public sealed interface ServerMessage {
    record Joined(String playerId, String token, String roomCode) implements ServerMessage {}
    record LobbyUpdate(List<String> nicks) implements ServerMessage {}
    record QuestionMsg(QuestionDto question) implements ServerMessage {}
    record AnswerAck(long questionId) implements ServerMessage {}
    record RoundResult(long questionId, long correctOptionId, Map<Long, Integer> distribution,
                       List<RoundLeaderboardEntryDto> leaderboard) implements ServerMessage {}
    record GameOver(List<RoundLeaderboardEntryDto> leaderboard) implements ServerMessage {}
    record RejoinSnapshot(RoomState roomState, QuestionDto question, boolean alreadyAnswered,
                          int myScore, List<RoundLeaderboardEntryDto> leaderboard) implements ServerMessage {}
    record ErrorMsg(ErrorDto error) implements ServerMessage {}
}
