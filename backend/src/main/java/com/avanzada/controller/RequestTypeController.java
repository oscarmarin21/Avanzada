package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.RequestTypeDto;
import com.avanzada.repository.RequestTypeRepository;
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
@Tag(name = "Request types", description = "Reference data: request types.")
public class RequestTypeController {

    private final RequestTypeRepository requestTypeRepository;
    private final RequestMapper mapper;

    @GetMapping("/request-types")
    @Operation(
            summary = "List request types",
            description = "Returns all request types that can be used when registering or classifying requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request types found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestTypeDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content)
    })
    public ResponseEntity<List<RequestTypeDto>> listRequestTypes() {
        List<RequestTypeDto> body = requestTypeRepository.findAll().stream()
                .map(mapper::toRequestTypeDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
