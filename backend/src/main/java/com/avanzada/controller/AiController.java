package com.avanzada.controller;

import com.avanzada.config.AiProperties;
import com.avanzada.dto.AiStatusDto;
import com.avanzada.dto.SuggestRequestDto;
import com.avanzada.dto.SuggestResponseDto;
import com.avanzada.service.AiService;
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
public class AiController {

    private final AiService aiService;
    private final AiProperties aiProperties;

    /**
     * Returns whether AI features are available (key set and enabled).
     * Frontend can hide "Suggest type (AI)" when available is false.
     */
    @GetMapping("/ai/status")
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
    public ResponseEntity<SuggestResponseDto> suggest(@Valid @RequestBody SuggestRequestDto dto) {
        SuggestResponseDto response = aiService.suggestTypeAndPriority(dto.getDescription());
        return ResponseEntity.ok(response);
    }
}
