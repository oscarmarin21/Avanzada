package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.StateDto;
import com.avanzada.repository.StateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "States", description = "Reference data: request states.")
public class StateController {

    private final StateRepository stateRepository;
    private final RequestMapper mapper;

    @GetMapping("/states")
    @Operation(
            summary = "List states",
            description = "Returns all request states ordered by display order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "States found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = StateDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content)
    })
    public ResponseEntity<List<StateDto>> listStates() {
        List<StateDto> body = stateRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(mapper::toStateDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
