package com.example.quizarenabackend.ws.messages;

import com.example.quizarenabackend.game.dtos.CurrentQuestionDto;
import com.example.quizarenabackend.game.dtos.RejoinSnapshotDto;
import com.example.quizarenabackend.game.dtos.RoundResultDto;
import com.example.quizarenabackend.game.entities.CloseReason;
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
        @JsonSubTypes.Type(value = ServerMessage.ErrorMsg.class, name = "error"),
        @JsonSubTypes.Type(value = ServerMessage.RoomClosed.class, name = "room_closed"),
        @JsonSubTypes.Type(value = ServerMessage.HostStatus.class, name = "host_status"),
})
public sealed interface ServerMessage {
    record Joined(String playerId, String token, String roomCode) implements ServerMessage {}
    record LobbyUpdate(List<String> nicks) implements ServerMessage {}
    record QuestionMsg(CurrentQuestionDto question) implements ServerMessage {}
    record AnswerAck(long questionId) implements ServerMessage {}
    record RoundResult(RoundResultDto roundResult) implements ServerMessage {}
    record GameOver(List<RoundLeaderboardEntryDto> leaderboard) implements ServerMessage {}
    record RejoinSnapshot(RejoinSnapshotDto snapshot) implements ServerMessage {}
    record HostStatus(boolean connected, Long closesAt) implements ServerMessage {}
    record ErrorMsg(ErrorDto error) implements ServerMessage {}
    record RoomClosed(CloseReason reason) implements ServerMessage {}
}
