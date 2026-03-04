package com.avanzada.controller;

import com.avanzada.config.AiProperties;
import com.avanzada.dto.AiStatusDto;
import com.avanzada.dto.SuggestRequestDto;
import com.avanzada.dto.SuggestResponseDto;
import com.avanzada.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Optional IA endpoints (RF-09, RF-10, RF-11). Best-effort; core flows do not depend on these.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "AI", description = "Optional AI helpers for summaries and suggestions.")
public class AiController {

    private final AiService aiService;
    private final AiProperties aiProperties;

    /**
     * Returns whether AI features are available (key set and enabled).
     * Frontend can hide "Suggest type (AI)" when available is false.
     */
    @GetMapping("/ai/status")
    @Operation(
            summary = "Get AI availability",
            description = "Returns whether AI features are enabled and configured. Frontend can hide AI features when not available."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status returned",
                    content = @Content(schema = @Schema(implementation = AiStatusDto.class)))
    })
    public ResponseEntity<AiStatusDto> status() {
        return ResponseEntity.ok(AiStatusDto.builder()
                .available(aiProperties.isConfigured())
                .build());
    }

    /**
     * Suggests request type and priority from description (RF-10).
     * Client must confirm or adjust before applying; suggestions are never auto-applied.
     */
    @PostMapping("/ai/suggest")
    @Operation(
            summary = "Suggest type and priority using AI",
            description = "Suggests request type and priority from a free-text description. Suggestions are never auto-applied."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suggestion generated",
                    content = @Content(schema = @Schema(implementation = SuggestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content)
    })
    public ResponseEntity<SuggestResponseDto> suggest(@Valid @RequestBody SuggestRequestDto dto) {
        SuggestResponseDto response = aiService.suggestTypeAndPriority(dto.getDescription());
        return ResponseEntity.ok(response);
    }
}
