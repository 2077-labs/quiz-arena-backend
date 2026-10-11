package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.exceptions.QuizNotFoundException;
import com.example.quizarenabackend.quiz.dtos.CreateQuizDto;
import com.example.quizarenabackend.quiz.dtos.QuizWithQuestionsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quizy", description = "Zarządzanie quizami")
public class QuizController {
    private final QuizService quizService;

    @PostMapping
    @Operation(summary = "Tworzenie nowego quizu", description = "Tworzy nowy quiz na podstawie przesłanych danych. Zwraca status 201 Created w przypadku powodzenia.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Quiz został pomyślnie utworzony"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Niepoprawne dane wejściowe lub quiz o tej samej nazwie już istnieje",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
            ),

    })
    public ResponseEntity<Void> createQuiz(@Valid @RequestBody CreateQuizDto dto) {
        quizService.createQuiz(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @PatchMapping("/{quizId}")
    @Operation(summary = "Aktualizacja istniejącego quizu", description = "Aktualizuje istniejący quiz na podstawie przesłanych danych. Zwraca status 200 OK w przypadku powodzenia.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Quiz został pomyślnie zaktualizowany"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Niepoprawne dane wejściowe lub quiz o tej samej nazwie już istnieje",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Quiz o podanym ID nie został znaleziony",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
            )
    })
    public ResponseEntity<Void> updateQuiz(@PathVariable Long quizId, @Valid @RequestBody CreateQuizDto dto) {
        quizService.updateQuiz(quizId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{quizId}")
    @Operation(summary = "Pobieranie quizu wraz z pytaniami", description = "Zwraca quiz wraz z jego pytaniami na podstawie podanego ID. Zwraca status 200 OK w przypadku powodzenia.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Quiz został pomyślnie pobrany",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuizWithQuestionsDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Quiz o podanym ID nie został znaleziony",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
            )
    })
    public ResponseEntity<QuizWithQuestionsDto> getQuizWithQuestions(@PathVariable Long quizId) {
        QuizWithQuestionsDto quizWithQuestionsDto = quizService.getQuizWithQuestions(quizId);
        return ResponseEntity.ok(quizWithQuestionsDto);
    }
}
