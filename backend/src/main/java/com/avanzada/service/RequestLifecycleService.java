package com.avanzada.service;

import com.avanzada.entity.*;
import com.avanzada.exception.InvalidStateTransitionException;
import com.avanzada.exception.RequestNotFoundException;
import com.avanzada.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Implements request lifecycle (state machine), prioritization, assignment, and history (RF-03, RF-04, RF-05, RF-06, RF-08).
 */
@Service
@RequiredArgsConstructor
public class RequestLifecycleService {

    private static final String REGISTRADA = "REGISTRADA";
    private static final String CLASIFICADA = "CLASIFICADA";
    private static final String EN_ATENCION = "EN_ATENCION";
    private static final String ATENDIDA = "ATENDIDA";
    private static final String CERRADA = "CERRADA";

    private final RequestRepository requestRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final ChannelRepository channelRepository;
    private final StateRepository stateRepository;
    private final UserRepository userRepository;
    private final HistoryEntryRepository historyEntryRepository;

    @Transactional
    public Request createRequest(String description, Long requestTypeId, Long channelId, Long requestedById, Instant registeredAt) {
        RequestType requestType = requestTypeRepository.findById(requestTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Request type not found: " + requestTypeId));
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        User requestedBy = userRepository.findById(requestedById)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestedById));
        State state = stateRepository.findByCode(REGISTRADA)
                .orElseThrow(() -> new IllegalStateException("State REGISTRADA not found; ensure reference data is loaded"));

        Instant now = registeredAt != null ? registeredAt : Instant.now();
        Request request = Request.builder()
                .description(description)
                .registeredAt(now)
                .requestType(requestType)
                .channel(channel)
                .state(state)
                .requestedBy(requestedBy)
                .build();
        request = requestRepository.save(request);
        appendHistory(request, "REGISTERED", requestedBy, "Request registered");
        return request;
    }

    /**
     * Classify request and set priority; transition REGISTRADA → CLASIFICADA (RF-02, RF-03, RF-04).
     */
    @Transactional
    public Request classify(Long requestId, Long requestTypeId, Priority priority, String priorityJustification, Long userId) {
        Request request = findRequestOrThrow(requestId);
        requireState(request, REGISTRADA, "classify");

        RequestType requestType = requestTypeRepository.findById(requestTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Request type not found: " + requestTypeId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        State clasificada = stateRepository.findByCode(CLASIFICADA)
                .orElseThrow(() -> new IllegalStateException("State CLASIFICADA not found"));

        request.setRequestType(requestType);
        request.setPriority(priority);
        request.setPriorityJustification(priorityJustification != null ? priorityJustification : "");
        request.setState(clasificada);
        request = requestRepository.save(request);
        appendHistory(request, "CLASSIFIED", user,
                "Type: " + requestType.getCode() + ", Priority: " + priority + (priorityJustification != null && !priorityJustification.isBlank() ? ". " + priorityJustification : ""));
        return request;
    }

    /**
     * Assign responsible; transition CLASIFICADA → EN_ATENCION (RF-05, RF-04). Validates assignee is active.
     */
    @Transactional
    public Request assign(Long requestId, Long assignedToId, Long userId) {
        Request request = findRequestOrThrow(requestId);
        requireState(request, CLASIFICADA, "assign");

        User assignee = userRepository.findById(assignedToId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + assignedToId));
        if (Boolean.FALSE.equals(assignee.getActive())) {
            throw new IllegalArgumentException("Cannot assign to inactive user: " + assignedToId);
        }
        User performingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        State enAtencion = stateRepository.findByCode(EN_ATENCION)
                .orElseThrow(() -> new IllegalStateException("State EN_ATENCION not found"));

        request.setAssignedTo(assignee);
        request.setState(enAtencion);
        request = requestRepository.save(request);
        appendHistory(request, "ASSIGNED", performingUser, "Assigned to " + assignee.getName() + " (" + assignee.getIdentifier() + ")");
        return request;
    }

    /**
     * Mark as attended; transition EN_ATENCION → ATENDIDA (RF-04).
     */
    @Transactional
    public Request attend(Long requestId, Long userId, String observations) {
        Request request = findRequestOrThrow(requestId);
        requireState(request, EN_ATENCION, "attend");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        State atendida = stateRepository.findByCode(ATENDIDA)
                .orElseThrow(() -> new IllegalStateException("State ATENDIDA not found"));

        request.setState(atendida);
        request = requestRepository.save(request);
        appendHistory(request, "ATTENDED", user, observations != null ? observations : "");
        return request;
    }

    /**
     * Close request; transition ATENDIDA → CERRADA only, with required closure observation (RF-08).
     */
    @Transactional
    public Request close(Long requestId, String closureObservation, Long userId) {
        Request request = findRequestOrThrow(requestId);
        requireState(request, ATENDIDA, "close");

        if (closureObservation == null || closureObservation.isBlank()) {
            throw new IllegalArgumentException("Closure observation is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        State cerrada = stateRepository.findByCode(CERRADA)
                .orElseThrow(() -> new IllegalStateException("State CERRADA not found"));

        request.setClosureObservation(closureObservation);
        request.setState(cerrada);
        request = requestRepository.save(request);
        appendHistory(request, "CLOSED", user, closureObservation);
        return request;
    }

    /**
     * Suggests a default priority based on request type (RF-03). Can be used when no priority is provided.
     */
    public Priority suggestPriorityByRequestType(RequestType requestType) {
        if (requestType == null || requestType.getCode() == null) {
            return Priority.MEDIUM;
        }
        switch (requestType.getCode().toUpperCase()) {
            case "HOMOLOG":
            case "CUPOS":
                return Priority.HIGH;
            case "CONSULTA":
                return Priority.LOW;
            default:
                return Priority.MEDIUM;
        }
    }

    public Request findRequestOrThrow(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found: " + requestId));
    }

    private void requireState(Request request, String expectedCode, String action) {
        if (request.getState() == null || !expectedCode.equals(request.getState().getCode())) {
            String current = request.getState() != null ? request.getState().getCode() : "null";
            if (CERRADA.equals(current)) {
                throw new InvalidStateTransitionException("Request is closed and cannot be modified");
            }
            throw new InvalidStateTransitionException(
                    "Cannot " + action + ": request is in state " + current + ", expected " + expectedCode);
        }
    }

    private void appendHistory(Request request, String action, User user, String observations) {
        HistoryEntry entry = HistoryEntry.builder()
                .request(request)
                .occurredAt(Instant.now())
                .action(action)
                .user(user)
                .observations(observations != null && !observations.isBlank() ? observations : null)
                .build();
        historyEntryRepository.save(entry);
    }
}
