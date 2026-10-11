package com.example.quizarenabackend.game;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.game.dtos.CreateRoomRequest;
import com.example.quizarenabackend.game.dtos.CreateRoomResponse;
import com.example.quizarenabackend.game.entities.RoomState;
import com.example.quizarenabackend.quiz.QuizRepository;
import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.Question;
import com.example.quizarenabackend.quiz.entities.Quiz;
import com.example.quizarenabackend.quiz.exceptons.QuizNotFoundException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestConstructor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class RoomIntegrationTest {
    private final TestRestTemplate rest;
    private final QuizRepository quizRepository;
    private final ObjectMapper mapper;

    @LocalServerPort
    private int port;
    private Long quizId;
    private Long noQuestionsQuizId;
    private final List<WebSocketSession> openSessions = new ArrayList<>();

    private static final int MAX_WAIT_TIME_SECONDS = 3;

    @BeforeEach
    void createQuiz() {
        Quiz quiz = new Quiz();
        quiz.setTitle("Test quiz");

        Question q = new Question();
        q.setText("2 + 2 = ?");
        q.setQuiz(quiz);

        AnswerOption four = new AnswerOption();
        four.setText("4");
        four.setCorrect(true);
        four.setQuestion(q);

        AnswerOption five = new AnswerOption();
        five.setText("5");
        five.setQuestion(q);

        q.getAnswerOptions().addAll(List.of(four, five));
        quiz.getQuestions().add(q);
        quizId = quizRepository.save(quiz).getId();

        Quiz noQuestionsQuiz = new Quiz();
        noQuestionsQuiz.setTitle("No questions quiz");
        noQuestionsQuizId = quizRepository.save(noQuestionsQuiz).getId();
    }

    @AfterEach
    void closeSockets() {
        for (WebSocketSession s : openSessions) {
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    // REST

    @Test
    void postRooms_createsRoomAndReturnsCredentials() {
        CreateRoomRequest request = new CreateRoomRequest(quizId, "Host");

        ResponseEntity<CreateRoomResponse> res = rest.postForEntity(
                "/api/rooms", request, CreateRoomResponse.class);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(4, res.getBody().getRoomCode().length());
        assertNotNull(res.getBody().getPlayerId());
        assertNotNull(res.getBody().getToken());
    }

    @Test
    void postRooms_unknownQuiz_returnsBadRequestWithErrorDto() {
        CreateRoomRequest request = new CreateRoomRequest(999_999L, "Host");

        ResponseEntity<ErrorDto> res = rest.postForEntity(
                "/api/rooms", request, ErrorDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(QuizNotFoundException.CODE, res.getBody().getCode());
    }

    @Test
    void postRooms_blankNick_returnsBadRequestWithValidationErrors() {
        CreateRoomRequest request = new CreateRoomRequest(quizId, " ");

        ResponseEntity<ErrorDto> res = rest.postForEntity(
                "/api/rooms", request, ErrorDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(ErrorDto.VALIDATION_ERROR_STATUS, res.getBody().getCode());

        assertTrue(res.getBody().getValidationErrors().containsKey("hostNick"));
    }

    @Test
    void postRooms_noQuestionsQuiz_returnsBadRequestWithErrorDto() {
        CreateRoomRequest request = new CreateRoomRequest(noQuestionsQuizId, "Host");

        ResponseEntity<ErrorDto> res = rest.postForEntity(
                "/api/rooms", request, ErrorDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("NO_QUESTIONS_QUIZ", res.getBody().getCode());
    }

    // WS

    @Test
    void hostAndPlayerJoin_receiveLobbyUpdates_andLeavingIsReported() throws Exception {
        CreateRoomResponse room = createRoom();
        TestClient host = connectHost(room);

        TestClient ala = connect();
        ala.send(Map.of("type", "join", "roomCode", room.getRoomCode(), "nick", "Ala"));

        JsonNode joined = ala.next();
        assertEquals("joined", joined.get("type").asString());
        assertFalse(joined.get("playerId").asString().isEmpty());
        assertFalse(joined.get("token").asString().isEmpty());
        assertEquals(room.getRoomCode(), joined.get("roomCode").asString());

        JsonNode alaLobby = ala.next();
        assertEquals("lobby_update", alaLobby.get("type").asString());
        assertEquals("Ala", alaLobby.get("nicks").get(0).asString());

        JsonNode hostLobby = host.next();
        assertEquals("lobby_update", hostLobby.get("type").asString());
        assertEquals(1, hostLobby.get("nicks").size());
        assertEquals("Ala", hostLobby.get("nicks").get(0).asString());

        ala.session.close();
        JsonNode afterLeave = host.next();
        assertEquals("lobby_update", afterLeave.get("type").asString());
        assertEquals(0, afterLeave.get("nicks").size());
    }

    @Test
    void join_unknownRoom_returnsError() throws Exception {
        TestClient c = connect();
        c.send(Map.of("type", "join", "roomCode", "ZZZZ", "nick", "Ala"));

        JsonNode err = c.next();
        assertEquals("error", err.get("type").asString());
        assertEquals("ROOM_NOT_FOUND", err.get("error").get("code").asString());
    }

    @Test
    void join_duplicateNick_returnsError() throws Exception {
        CreateRoomResponse room = createRoom();
        connectHost(room);

        TestClient ala = connect();
        ala.send(Map.of("type", "join", "roomCode", room.getRoomCode(), "nick", "Ala"));
        ala.next(); // joined
        ala.next(); // lobby_update

        TestClient bob = connect();
        bob.send(Map.of("type", "join", "roomCode", room.getRoomCode(), "nick", "ala"));

        JsonNode err = bob.next();
        assertEquals("error", err.get("type").asString());
        assertEquals("NICK_TAKEN", err.get("error").get("code").asString());
    }

    @Test
    void rejoin_withWrongToken_returnsError() throws Exception {
        CreateRoomResponse room = createRoom();
        TestClient c = connect();
        c.send(Map.of("type", "rejoin", "roomCode", room.getRoomCode(),
                "playerId", room.getPlayerId(), "token", "zly-token"));

        JsonNode err = c.next();
        assertEquals("error", err.get("type").asString());
        assertEquals("INVALID_PLAYER_OR_TOKEN_REJOIN", err.get("error").get("code").asString());
    }

    // helpers

    private CreateRoomResponse createRoom() {
        CreateRoomRequest request = new CreateRoomRequest(quizId, "Host");
        ResponseEntity<CreateRoomResponse> res = rest.postForEntity(
                "/api/rooms", request, CreateRoomResponse.class);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        return res.getBody();
    }

    // Connects the host to the room and asserts the two initial messages: rejoin_snapshot and lobby_update.
    private TestClient connectHost(CreateRoomResponse room) throws Exception {
        TestClient host = connect();
        host.send(Map.of("type", "rejoin", "roomCode", room.getRoomCode(),
                "playerId", room.getPlayerId(), "token", room.getToken()));

        JsonNode snapshot = host.next();
        assertEquals("rejoin_snapshot", snapshot.get("type").asString());
        assertEquals(RoomState.LOBBY.toString(), snapshot.get("roomState").asString());

        JsonNode lobby = host.next();
        assertEquals("lobby_update", lobby.get("type").asString());
        assertEquals(0, lobby.get("nicks").size());
        return host;
    }

    private TestClient connect() throws Exception {
        TestClient client = new TestClient();
        client.session = new StandardWebSocketClient()
                .execute(client, "ws://localhost:" + port + "/ws")
                .get(5, TimeUnit.SECONDS);
        openSessions.add(client.session);
        return client;
    }

    // Collects messages from the server in a queue and allows for sending messages to the server. The next() method waits
    // for a message to arrive in the queue and returns it, or fails if no message arrives within the desired number of seconds.
    private class TestClient extends TextWebSocketHandler {
        final BlockingQueue<JsonNode> inbox = new LinkedBlockingQueue<>();
        WebSocketSession session;

        @Override
        protected void handleTextMessage(WebSocketSession s, TextMessage message) throws Exception {
            inbox.add(mapper.readTree(message.getPayload()));
        }

        void send(Map<String, ?> payload) throws IOException {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
        }

        JsonNode next() throws InterruptedException {
            JsonNode n = inbox.poll(MAX_WAIT_TIME_SECONDS, TimeUnit.SECONDS);
            assertNotNull(n, "Serwer nie odpowiedział w ciągu " + MAX_WAIT_TIME_SECONDS + " s");
            return n;
        }
    }
}