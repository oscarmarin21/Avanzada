package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.*;
import com.avanzada.entity.Priority;
import com.avanzada.entity.Request;
import com.avanzada.repository.HistoryEntryRepository;
import com.avanzada.repository.RequestRepository;
import com.avanzada.repository.StateRepository;
import com.avanzada.service.RequestLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RequestController {

    private final RequestLifecycleService lifecycleService;
    private final RequestRepository requestRepository;
    private final StateRepository stateRepository;
    private final HistoryEntryRepository historyEntryRepository;
    private final RequestMapper mapper;

    private static final String HEADER_USER_ID = "X-User-Id";

    @PostMapping("/requests")
    public ResponseEntity<RequestResponseDto> createRequest(@Valid @RequestBody CreateRequestDto dto) {
        Instant registeredAt = parseInstant(dto.getRegisteredAt());
        Request request = lifecycleService.createRequest(
                dto.getDescription(),
                dto.getRequestTypeId(),
                dto.getChannelId(),
                dto.getRequestedById(),
                registeredAt);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRequestResponseDto(request));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<RequestResponseDto>> listRequests(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Long requestType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedTo) {
        Long stateId = null;
        if (state != null && !state.isBlank()) {
            stateId = stateRepository.findByCode(state.trim()).map(s -> s.getId()).orElse(null);
        }
        Priority priorityEnum = null;
        if (priority != null && !priority.isBlank()) {
            try {
                priorityEnum = Priority.valueOf(priority.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // invalid priority -> no filter
            }
        }
        List<Request> list = requestRepository.findByFilters(stateId, requestType, priorityEnum, assignedTo);
        List<RequestResponseDto> body = list.stream().map(mapper::toRequestResponseDto).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<RequestResponseDto> getRequest(@PathVariable Long id) {
        Request request = lifecycleService.findRequestOrThrow(id);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PostMapping("/requests/{id}/classify")
    public ResponseEntity<RequestResponseDto> classify(
            @PathVariable Long id,
            @Valid @RequestBody ClassifyRequestDto dto,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long headerUserId) {
        Priority priority = parsePriority(dto.getPriority());
        Long userId = headerUserId != null ? headerUserId : lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        Request request = lifecycleService.classify(id, dto.getRequestTypeId(), priority, dto.getPriorityJustification(), userId);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PostMapping("/requests/{id}/assign")
    public ResponseEntity<RequestResponseDto> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequestDto dto,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long headerUserId) {
        Long userId = headerUserId != null ? headerUserId : lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        Request request = lifecycleService.assign(id, dto.getAssignedToId(), userId);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PostMapping("/requests/{id}/attend")
    public ResponseEntity<RequestResponseDto> attend(
            @PathVariable Long id,
            @RequestBody(required = false) AttendRequestDto dto,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long headerUserId) {
        Long userId = headerUserId != null ? headerUserId : lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        String observations = dto != null ? dto.getObservations() : null;
        Request request = lifecycleService.attend(id, userId, observations);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PostMapping("/requests/{id}/close")
    public ResponseEntity<RequestResponseDto> close(
            @PathVariable Long id,
            @Valid @RequestBody CloseRequestDto dto,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long headerUserId) {
        Long userId = headerUserId != null ? headerUserId : lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        Request request = lifecycleService.close(id, dto.getClosureObservation(), userId);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @GetMapping("/requests/{id}/history")
    public ResponseEntity<List<HistoryEntryDto>> getHistory(@PathVariable Long id) {
        lifecycleService.findRequestOrThrow(id); // 404 if not found
        List<HistoryEntryDto> body = historyEntryRepository.findByRequest_IdOrderByOccurredAtDesc(id).stream()
                .map(mapper::toHistoryEntryDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

    private static Priority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("priority is required");
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority: " + value + ". Must be LOW, MEDIUM, or HIGH");
        }
    }
}
