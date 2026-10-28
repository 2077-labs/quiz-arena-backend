package com.example.quizarenabackend.game;

import com.example.quizarenabackend.game.entities.*;
import com.example.quizarenabackend.game.exceptions.*;
import com.example.quizarenabackend.quiz.GameResultRepository;
import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.GameResult;
import com.example.quizarenabackend.quiz.entities.Question;
import com.example.quizarenabackend.ws.MessageSender;
import com.example.quizarenabackend.ws.QuizArenaWsProperties;
import com.example.quizarenabackend.ws.messages.ClientMessage;
import com.example.quizarenabackend.ws.messages.ServerMessage;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private final MessageSender sender;
    private final ScoringService scoringService;
    private final GameMessageFactory messages;
    private final GameResultRepository gameResultRepository;
    private final QuizArenaWsProperties properties;

    public void startGame(Room room, Player requester) {
        requireHost(room, requester);
        if (room.getState() != RoomState.LOBBY) {
            throw new InvalidRoomStateException(room.getState(), InvalidRoomStateExceptionReason.STARTING);
        }
        if (messages.participants(room).isEmpty()) {
            throw new NoPlayersException();
        }
    }

    public void submitAnswer(Room room, Player player, ClientMessage.SubmitAnswer msg) {
        if (room.getState() != RoomState.QUESTION) {
            throw new InvalidRoomStateException(room.getState(), InvalidRoomStateExceptionReason.ANSWERING);
        }

        Round round = room.getRound();
        Question question = round.getQuestion();
        if (question.getId() == null || question.getId() != msg.questionId()) {
            throw new QuestionMismatchException();
        }

        long now = System.currentTimeMillis();
        if (now > round.getEndsAt() +properties.getAnswerGrace().toMillis()) {
            // The timer is late, therefore we close the round right now and reject the answer. The important thing is
            // that there is a grace period for the answer, so that the client can send the answer even if the timer is late.
            endRound(room, round);
            throw new InvalidRoomStateException(room.getState(), InvalidRoomStateExceptionReason.ANSWERING);
        }

        AnswerOption option = question.getAnswerOptions().stream()
                .filter(o->o.getId()==msg.optionId())
                .findFirst()
                .orElseThrow(InvalidOptionException::new);

        // We calculate the points and save the answer in the round state, though we don't send the result to the client yet,
        // as well as we don't update the player's points yet, because the round is still ongoing. The points will be sent
        // to the client and updated in the player's state when the round ends.
        int points = scoringService.calculateScore(question, now - round.getStartedAt(), option.isCorrect());
        boolean first = round.submit(player.getId(), new Answer(option.getId(), now, option.isCorrect(), points));
        sender.send(player.getSession(), new ServerMessage.AnswerAck(question.getId()));

        if (first) {
            endRoundIfEveryoneAnswered(room, round);
        }
    }

    public void nextQuestion(Room room, Player requester) {
        requireHost(room, requester);
        if (room.getState() != RoomState.RESULT) {
            throw new InvalidRoomStateException(room.getState(), InvalidRoomStateExceptionReason.ADVANCING);
        }
        if (room.hasNextQuestion()) {
            startQuestion(room);
        } else {
            finishGame(room);
        }
    }
    // Not sure whether we should immediately check whether everyone has answered when someone leaves and proceed
    // with the round, or whether there should be some sort of delay.
    public void onParticipantDisconnected(Room room) {
        if (room.getState() == RoomState.QUESTION) {
            endRoundIfEveryoneAnswered(room, room.getRound());
        }
    }

    private void startQuestion(Room room) {
        int index = room.getQuestionIndex() + 1;
        Question question = room.getQuiz().getQuestions().get(index);
        long now = System.currentTimeMillis();
        Round round = new Round(question, now);

        room.setQuestionIndex(index);
        room.setRound(round);
        room.setState(RoomState.QUESTION);
        room.touch();

        broadcast(room, new ServerMessage.QuestionMsg(messages.question(room, round, now)));
        long delay = round.getEndsAt() - now + properties.getAnswerGrace().toMillis();
        room.setRoundTimer(room.schedule(()->endRound(room, round), delay));
    }

    private void endRoundIfEveryoneAnswered(Room room, Round round) {
        List<Player> activePlayers = messages.participants(room).stream()
                .filter(p->p.isConnected())
                .toList();
        boolean everyoneAnswered = !activePlayers.isEmpty() && activePlayers.stream()
                .allMatch(p->round.getAnswers().containsKey(p.getId()));
        if (everyoneAnswered) {
            endRound(room, round);
        }
    }

    private void endRound(Room room, Round round) {
        if (room.getState() != RoomState.QUESTION || room.getRound() != round) {
            return;
        }
        // Cancel the timer, because we are ending the round now, and we don't want the timer to fire later.
        Room.cancel(room.getRoundTimer());
        room.setRoundTimer(null);

        // As it was mentioned in the submitAnswer method, we now update the players' scores and send the round result to everyone.
        applyPoints(room, round);
        room.setState(RoomState.RESULT);
        room.touch();
        // Also we process the round summary only once as it is the same for all players, and we then supply it with
        // player specific data and deliver to each player.
        RoundSummary summary = messages.summarize(room, round);
        round.setRoundSummary(summary);
        room.getPlayers().values().forEach(p-> {
            sender.send(p.getSession(), new ServerMessage.RoundResult(messages.resultFor(summary, round, p)));
        });
    }

    private static void applyPoints(Room room, Round round) {
        round.getAnswers().forEach((playerId, answer) -> {
            Player player = room.getPlayers().get(playerId);
            if (player != null) {
                player.setScore(player.getScore() + answer.points());
            }
        });
    }

    private void finishGame(Room room) {
        room.setState(RoomState.FINISHED);
        room.touch();
        saveResults(room);
        broadcast(room, new ServerMessage.GameOver(messages.leaderboard(room)));
    }

    private void saveResults(Room room) {
        try {
            Instant playedAt = Instant.now();
            List<GameResult> results = messages.participants(room).stream()
                    .map(p -> {
                        GameResult result = new GameResult();
                        result.setRoomCode(room.getCode());
                        result.setQuiz(room.getQuiz());
                        result.setNick(p.getNick());
                        result.setScore(p.getScore());
                        result.setPlayedAt(playedAt);
                        return result;
                    }).toList();
            gameResultRepository.saveAll(results);
        } catch (Exception ex) {
            log.error("Nie udało się zapisać wyników gry dla pokoju {}: {}", room.getCode(), ex.getMessage(), ex);
        }
    }

    private void requireHost(Room room, Player player) {
        if (!room.isHost(player)) {
            throw new NotHostException();
        }
    }

    private void broadcast(Room room, ServerMessage msg) {
        room.getPlayers().values().forEach(p -> sender.send(p.getSession(), msg));
    }
}
