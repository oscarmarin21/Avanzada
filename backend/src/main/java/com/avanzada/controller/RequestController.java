package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.*;
import com.avanzada.entity.Request;
import com.avanzada.security.AppUserDetails;
import com.avanzada.service.AiService;
import com.avanzada.service.RequestLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RequestController {

    private final RequestLifecycleService lifecycleService;
    private final RequestMapper mapper;
    private final AiService aiService;

    private static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) return null;
        return details.userId();
    }

    /** True if the current user has role STUDENT (they may only see their own requests). */
    private static boolean isCurrentUserStudent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_STUDENT"::equals);
    }

    private static boolean isCurrentUserAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    @PostMapping("/requests")
    public ResponseEntity<RequestResponseDto> createRequest(@Valid @RequestBody CreateRequestDto dto) {
        Long currentId = currentUserId();
        if (currentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long requestedById = currentId;
        if (isCurrentUserAdmin() && dto.getRequestedById() != null) {
            requestedById = dto.getRequestedById();
        }
        Request request = lifecycleService.createRequest(
                dto.getDescription(),
                dto.getRequestTypeId(),
                dto.getChannelId(),
                requestedById,
                dto.getRegisteredAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRequestResponseDto(request));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<RequestResponseDto>> listRequests(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Long requestType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedTo) {
        Long requestedById = isCurrentUserStudent() ? currentUserId() : null;
        List<Request> list = lifecycleService.listByFilters(state, requestType, priority, assignedTo, requestedById);
        List<RequestResponseDto> body = list.stream().map(mapper::toRequestResponseDto).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<RequestResponseDto> getRequest(@PathVariable Long id) {
        Request request = lifecycleService.findRequestOrThrow(id);
        if (isCurrentUserStudent() && !request.getRequestedBy().getId().equals(currentUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/requests/{id}/classify")
    public ResponseEntity<RequestResponseDto> classify(
            @PathVariable Long id,
            @Valid @RequestBody ClassifyRequestDto dto) {
        Long userId = currentUserId();
        if (userId == null) userId = lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        Request request = lifecycleService.classify(id, dto.getRequestTypeId(), dto.getPriority(), dto.getPriorityJustification(), userId);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/requests/{id}/assign")
    public ResponseEntity<RequestResponseDto> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequestDto dto) {
        Long userId = currentUserId();
        if (userId == null) userId = lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        Request request = lifecycleService.assign(id, dto.getAssignedToId(), userId);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/requests/{id}/attend")
    public ResponseEntity<RequestResponseDto> attend(
            @PathVariable Long id,
            @RequestBody(required = false) AttendRequestDto dto) {
        Long userId = currentUserId();
        if (userId == null) userId = lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        String observations = dto != null ? dto.getObservations() : null;
        Request request = lifecycleService.attend(id, userId, observations);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/requests/{id}/close")
    public ResponseEntity<RequestResponseDto> close(
            @PathVariable Long id,
            @Valid @RequestBody CloseRequestDto dto) {
        Long userId = currentUserId();
        if (userId == null) userId = lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        Request request = lifecycleService.close(id, dto.getClosureObservation(), userId);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @GetMapping("/requests/{id}/history")
    public ResponseEntity<List<HistoryEntryDto>> getHistory(@PathVariable Long id) {
        Request request = lifecycleService.findRequestOrThrow(id);
        if (isCurrentUserStudent() && !request.getRequestedBy().getId().equals(currentUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<HistoryEntryDto> body = lifecycleService.listHistory(id).stream()
                .map(mapper::toHistoryEntryDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    /**
     * Optional: get a textual summary of the request and its history (RF-09). Best-effort; returns fallback when IA unavailable.
     */
    @GetMapping("/requests/{id}/summary")
    public ResponseEntity<SummaryResponseDto> getSummary(@PathVariable Long id) {
        Request request = lifecycleService.findRequestOrThrow(id);
        if (isCurrentUserStudent() && !request.getRequestedBy().getId().equals(currentUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        SummaryResponseDto body = aiService.generateSummary(request);
        return ResponseEntity.ok(body);
    }
}
