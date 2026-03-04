package com.avanzada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "Health check endpoint.")
public class HealthController {

    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Simple health check used by Docker and external monitoring."
    )
    @ApiResponse(responseCode = "200", description = "Service is up",
            content = @Content(schema = @Schema(implementation = java.util.Map.class)))
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "backend"
        ));
    }
}
