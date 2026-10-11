package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.dtos.PageableResponse;
import com.example.quizarenabackend.quiz.dtos.*;
import com.example.quizarenabackend.quiz.entities.GameResult;
import com.example.quizarenabackend.quiz.entities.Quiz;
import com.example.quizarenabackend.quiz.exceptons.QuizNotFoundException;
import com.example.quizarenabackend.quiz.exceptons.SameTitleQuizExistsException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class QuizIntegrationTest {
    private final TestRestTemplate rest;
    private final QuizRepository quizRepository;
    private final GameResultRepository gameResultRepository;
    private final QuizService quizService;

    @BeforeEach
    void clearDatabase() {
        quizRepository.deleteAll();
    }

    @Test
    void createQuiz_ValidPayload_SavesQuizWithCorrectPositions() {
        CreateQuizDto payload = createSampleQuizDto("Geografia");

        ResponseEntity<Void> response = rest.postForEntity("/api/quizzes", payload, Void.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        List<Quiz> savedQuizzes = quizRepository.findAll();
        assertEquals(1, savedQuizzes.size());
        var retrievedQuiz = quizService.getQuizWithQuestions(savedQuizzes.get(0).getId());
        if (retrievedQuiz == null) {
            fail("Quiz not found after creation");
        }
        var savedQuiz = retrievedQuiz;
        assertEquals("Geografia", savedQuiz.getTitle());

        assertEquals(2, savedQuiz.getQuestions().size());
        assertEquals(0, savedQuiz.getQuestions().get(0).getPosition());
        assertEquals("Stolica Francji?", savedQuiz.getQuestions().get(0).getText());
        assertEquals(1, savedQuiz.getQuestions().get(1).getPosition());
        assertEquals("Stolica Niemiec?", savedQuiz.getQuestions().get(1).getText());

        assertEquals(0, savedQuiz.getQuestions().get(0).getAnswerOptions().get(0).getPosition());
        assertEquals(1, savedQuiz.getQuestions().get(0).getAnswerOptions().get(1).getPosition());
    }

    @Test
    void createQuiz_DuplicateTitle_ReturnsBadRequest() {
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Geografia"), Void.class);

        ResponseEntity<ErrorDto> response = rest.postForEntity("/api/quizzes", createSampleQuizDto("Geografia"), ErrorDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(SameTitleQuizExistsException.CODE, response.getBody().getCode());
    }

    @Test
    // Here we check whether an existing quiz
    void getQuizWithQuestions_ExistingQuiz_ReturnsCorrectDtoOrder() {
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Geografia"), Void.class);
        Long savedQuizId = quizRepository.findAll().getFirst().getId();

        ResponseEntity<QuizWithQuestionsDto> response = rest.getForEntity("/api/quizzes/" + savedQuizId, QuizWithQuestionsDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        QuizWithQuestionsDto dto = response.getBody();
        assertNotNull(dto);
        assertEquals(savedQuizId, dto.getId());
        assertEquals("Geografia", dto.getTitle());

        assertEquals(2, dto.getQuestions().size());
        assertEquals("Stolica Francji?", dto.getQuestions().get(0).getText());
        assertEquals("Stolica Niemiec?", dto.getQuestions().get(1).getText());
    }

    @Test
    void updateQuiz_ValidPayload_ReplacesQuestionsAndPreservesQuizId() {
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Historia"), Void.class);
        Quiz initialQuiz = quizRepository.findAll().get(0);
        Long quizId = initialQuiz.getId();

        CreateQuizDto updatePayload = new CreateQuizDto();
        updatePayload.setTitle("Historia (Zaktualizowana)");
        updatePayload.setDescription("Nowy opis");

        CreateQuestionDto newQuestion = new CreateQuestionDto();
        newQuestion.setText("Kiedy była bitwa pod Grunwaldem?");
        newQuestion.setOptions(List.of(
                new CreateQuestionAnswerDto("1410", true, null),
                new CreateQuestionAnswerDto("1420", false, null)
        ));
        updatePayload.setQuestions(List.of(newQuestion));

        ResponseEntity<Void> response = rest.exchange(
                "/api/quizzes/" + quizId,
                HttpMethod.PATCH,
                new HttpEntity<>(updatePayload),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        var quizCount = quizRepository.count();
        assertEquals(1, quizCount, "Nie powinien utworzyć się nowy quiz");

        Quiz updatedQuiz = quizRepository.findQuizWithQuestionsById(quizId).orElseThrow(() -> new AssertionError("Quiz nie został znaleziony po aktualizacji"));
        assertEquals(quizId, updatedQuiz.getId(), "ID quizu musi pozostać bez zmian");
        assertEquals("Historia (Zaktualizowana)", updatedQuiz.getTitle());

        assertEquals(1, updatedQuiz.getQuestions().size());
        assertEquals("Kiedy była bitwa pod Grunwaldem?", updatedQuiz.getQuestions().get(0).getText());
        assertEquals(0, updatedQuiz.getQuestions().get(0).getPosition());
    }

    @Test
    void updateQuiz_NonExistentId_ReturnsNotFound() {
        ResponseEntity<ErrorDto> response = rest.exchange(
                "/api/quizzes/9999",
                HttpMethod.PATCH,
                new HttpEntity<>(createSampleQuizDto("Test")),
                ErrorDto.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QuizNotFoundException.CODE, response.getBody().getCode());
    }

    @Test
    void getQuizzes_WithSearchText_ReturnsFilteredAndPaginatedQuizzes() {
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Zwierzęta Polski", "Niedźwiedzie i wilki"), Void.class);
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Zwierzęta Afryki", "Słonie i lwy"), Void.class);
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Geografia Europy", "Stolice i rzeki"), Void.class);

        ResponseEntity<PageableResponse<QuizDto>> response = rest.exchange(
                "/api/quizzes?searchText=zwierzęta&page=0&size=5",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PageableResponse<QuizDto>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getTotalElements(), "Powinno znaleźć dokładnie 2 quizy ze słowem 'zwierzęta'");
        assertEquals(2, response.getBody().getContent().size());

        boolean hasPolska = response.getBody().getContent().stream().anyMatch(q -> q.getTitle().equals("Zwierzęta Polski"));
        boolean hasAfryka = response.getBody().getContent().stream().anyMatch(q -> q.getTitle().equals("Zwierzęta Afryki"));
        assertTrue(hasPolska && hasAfryka);
    }

    @Test
    void getTrendingQuizzes_WithRecentGames_ReturnsTrendingBasedOnGameResults() {
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Quiz Popularny 1"), Void.class);
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Quiz Popularny 2"), Void.class);
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Quiz Niszowy"), Void.class);

        List<Quiz> quizzes = quizRepository.findAll();
        Quiz q1 = quizzes.get(0);
        Quiz q2 = quizzes.get(1);
        Quiz q3 = quizzes.get(2);

        addGameResults(q2, 3, Instant.now());
        addGameResults(q1, 1, Instant.now());
        ResponseEntity<List<QuizDto>> response = rest.exchange(
                "/api/quizzes/trending?limit=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<QuizDto>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size(), "Zwrócono maksymalnie 2 elementy zgodnie z limitem");

        assertEquals("Quiz Popularny 2", response.getBody().get(0).getTitle());
        assertEquals("Quiz Popularny 1", response.getBody().get(1).getTitle());
    }

    @Test
    void getTrendingQuizzes_NoRecentGames_ReturnsFallbackBasedOnRunCount() {
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Klasyk Wszechczasów"), Void.class);
        rest.postForEntity("/api/quizzes", createSampleQuizDto("Nowy ale słaby"), Void.class);

        List<Quiz> quizzes = quizRepository.findAll();
        Quiz qKlasyk = quizzes.get(0);
        Quiz qNowy = quizzes.get(1);

        // beyond the 7-day window, so it should not be counted in trending
        addGameResults(qNowy, 5, Instant.now().minus(10, ChronoUnit.DAYS));

        // because there won't be any quizzes with recent game results, the system should rely on runCount
        qKlasyk.setRunCount(100L);
        qNowy.setRunCount(5L);
        quizRepository.saveAll(List.of(qKlasyk, qNowy));
        ResponseEntity<List<QuizDto>> response = rest.exchange(
                "/api/quizzes/trending",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<QuizDto>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 2);

        assertEquals("Klasyk Wszechczasów", response.getBody().get(0).getTitle());
        assertEquals("Nowy ale słaby", response.getBody().get(1).getTitle());
    }

    private CreateQuizDto createSampleQuizDto(String title) {
        CreateQuizDto dto = new CreateQuizDto();
        dto.setTitle(title);
        dto.setDescription("Opis testowy");

        CreateQuestionDto q1 = new CreateQuestionDto();
        q1.setText("Stolica Francji?");
        q1.setOptions(List.of(
                new CreateQuestionAnswerDto("Paryż", true, null),
                new CreateQuestionAnswerDto("Londyn", false, null)
        ));

        CreateQuestionDto q2 = new CreateQuestionDto();
        q2.setText("Stolica Niemiec?");
        q2.setOptions(List.of(
                new CreateQuestionAnswerDto("Berlin", true, null),
                new CreateQuestionAnswerDto("Wiedeń", false, null)
        ));

        dto.setQuestions(List.of(q1, q2));
        return dto;
    }

    private CreateQuizDto createSampleQuizDto(String title, String description) {
        CreateQuizDto dto = createSampleQuizDto(title);
        dto.setDescription(description);
        return dto;
    }

    private void addGameResults(Quiz quiz, int count, Instant playedAt) {
        List<GameResult> resultsToSave = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GameResult gr = new GameResult();
            gr.setRoomCode("TEST" + i);
            gr.setNick("Player" + i);
            gr.setQuiz(quiz);
            gr.setScore(1000);
            gr.setPlayedAt(playedAt);
            gr.setRoomSessionId(UUID.randomUUID().toString());

            resultsToSave.add(gr);
        }
        gameResultRepository.saveAll(resultsToSave);
    }
}