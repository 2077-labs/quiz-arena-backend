package com.example.quizarenabackend.ws.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientMessage.Join.class, name = "join"),
        @JsonSubTypes.Type(value = ClientMessage.Rejoin.class, name = "rejoin"),
        @JsonSubTypes.Type(value = ClientMessage.StartGame.class, name = "start_game"),
        @JsonSubTypes.Type(value = ClientMessage.SubmitAnswer.class, name = "submit_answer"),
        @JsonSubTypes.Type(value = ClientMessage.NextQuestion.class, name = "next_question")
})
public sealed interface ClientMessage {
    record Join(String roomCode, String nick) implements ClientMessage {}
    record Rejoin(String roomCode, String playerId, String token) implements ClientMessage {}
    record StartGame() implements ClientMessage {}
    record SubmitAnswer(long questionId, long optionId) implements ClientMessage {}
    record NextQuestion() implements ClientMessage {}
}
