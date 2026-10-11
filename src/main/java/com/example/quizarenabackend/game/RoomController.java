package com.example.quizarenabackend.game;

import com.example.quizarenabackend.dtos.ErrorDto;
import com.example.quizarenabackend.game.dtos.CreateRoomRequest;
import com.example.quizarenabackend.game.dtos.CreateRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Pokoje", description = "Zarządzanie pokojami do gry")
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Tworzenie nowego pokoju", description = "Tworzy nowy pokój na podstawie przesłanych danych. Zwraca status 201 Created w przypadku powodzenia.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Pokój został pomyślnie utworzony",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateRoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Quiz o podanym ID nie istnieje lub nie ma pytań",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
            ),
    })
    public ResponseEntity<CreateRoomResponse> create(@Valid @RequestBody CreateRoomRequest createRoomRequest) {
        return ResponseEntity
                .status(201)
                .body(roomService.createRoom(createRoomRequest.getQuizId(), createRoomRequest.getHostNick()));
    }
}
