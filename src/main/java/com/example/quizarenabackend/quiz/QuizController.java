package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.dtos.PageableResponse;
import com.example.quizarenabackend.quiz.dtos.CreateQuizDto;
import com.example.quizarenabackend.quiz.dtos.GetQuizzesRequest;
import com.example.quizarenabackend.quiz.dtos.QuizDto;
import com.example.quizarenabackend.quiz.dtos.QuizWithQuestionsDto;
import com.example.quizarenabackend.quiz.entities.Quiz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        QuizWithQuestionsDto quizWithQuestionsDto = quizService.getQuizWithQuestionsDto(quizId);
        return ResponseEntity.ok(quizWithQuestionsDto);
    }

    private static class PagedQuizzesResponse extends PageableResponse<QuizDto> {
    }
    @Operation(summary = "Pobieranie listy quizów", description = "Zwraca listę quizów na podstawie przesłanych parametrów. Zwraca status 200 OK w przypadku powodzenia.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista quizów została pomyślnie pobrana",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedQuizzesResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<PageableResponse<QuizDto>> getQuizzes(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @ParameterObject GetQuizzesRequest request
    ) {
        var page = quizService.getQuizzes(request, pageable);
        return ResponseEntity.ok(PageableResponse.of(page));
    }

    private interface TrendingQuizzesResponse extends List<QuizDto> {
    }
    @Operation(summary = "Pobieranie listy najpopularniejszych quizów", description = "Zwraca listę najpopularniejszych quizów na podstawie liczby uruchomień. Zwraca status 200 OK w przypadku powodzenia.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista najpopularniejszych quizów została pomyślnie pobrana",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TrendingQuizzesResponse.class))
            )
    })
    @GetMapping("/trending")
    public ResponseEntity<List<QuizDto>> getTrendingQuizzes(
            @Parameter(description = "Maksymalna liczba quizów do pobrania")
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<QuizDto> trending = quizService.getTrendingQuizzes(limit);
        return ResponseEntity.ok(trending);
    }
}
