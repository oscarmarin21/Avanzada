package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.*;
import com.avanzada.entity.Request;
import com.avanzada.security.AppUserDetails;
import com.avanzada.service.AiService;
import com.avanzada.service.RequestLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Requests", description = "Operations to create, view and manage support requests.")
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
    @Operation(
            summary = "Create a new request",
            description = "Creates a new support request. Students can only create requests for themselves; admins may specify another requester."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid data",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User not allowed to create request",
                    content = @Content)
    })
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
    @Operation(
            summary = "List requests",
            description = "Lists requests applying optional filters. Students only see their own requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requests found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content)
    })
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
    @Operation(
            summary = "Get request by id",
            description = "Returns a single request by id. Students can only access their own requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request found",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Student trying to access another user's request",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content)
    })
    public ResponseEntity<RequestResponseDto> getRequest(@PathVariable Long id) {
        Request request = lifecycleService.findRequestOrThrow(id);
        if (isCurrentUserStudent() && !request.getRequestedBy().getId().equals(currentUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/requests/{id}/classify")
    @Operation(
            summary = "Classify a request",
            description = "Sets type and priority of a request. Only STAFF and ADMIN can classify."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request classified",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid data",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User not allowed to classify",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content)
    })
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
    @Operation(
            summary = "Assign a request to a staff member",
            description = "Assigns a request to a specific staff user. Only STAFF and ADMIN can assign."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request assigned",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid data",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User not allowed to assign",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content)
    })
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
    @Operation(
            summary = "Mark request as attended",
            description = "Marks a request as attended, with optional observations. Only STAFF and ADMIN can attend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request attended",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid data",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User not allowed to attend",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content)
    })
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
    @Operation(
            summary = "Close a request",
            description = "Closes a request with a closure observation. Only ADMIN can close."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request closed",
                    content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid data",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "User not allowed to close",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content)
    })
    public ResponseEntity<RequestResponseDto> close(
            @PathVariable Long id,
            @Valid @RequestBody CloseRequestDto dto) {
        Long userId = currentUserId();
        if (userId == null) userId = lifecycleService.findRequestOrThrow(id).getRequestedBy().getId();
        Request request = lifecycleService.close(id, dto.getClosureObservation(), userId);
        return ResponseEntity.ok(mapper.toRequestResponseDto(request));
    }

    @GetMapping("/requests/{id}/history")
    @Operation(
            summary = "Get request history",
            description = "Returns the history entries for a request. Students can only access the history of their own requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History entries found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HistoryEntryDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Student trying to access another user's request",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content)
    })
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
    @Operation(
            summary = "Get request summary (AI or fallback)",
            description = "Returns a textual summary of the request and its history using AI when available, or a fallback summary otherwise."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary generated",
                    content = @Content(schema = @Schema(implementation = SummaryResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Student trying to access another user's request",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content)
    })
    public ResponseEntity<SummaryResponseDto> getSummary(@PathVariable Long id) {
        Request request = lifecycleService.findRequestOrThrow(id);
        if (isCurrentUserStudent() && !request.getRequestedBy().getId().equals(currentUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        SummaryResponseDto body = aiService.generateSummary(request);
        return ResponseEntity.ok(body);
    }
}
