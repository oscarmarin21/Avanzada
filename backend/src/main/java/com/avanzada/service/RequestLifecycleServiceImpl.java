package com.avanzada.service;

import com.avanzada.entity.Channel;
import com.avanzada.entity.HistoryEntry;
import com.avanzada.entity.Priority;
import com.avanzada.entity.Request;
import com.avanzada.entity.RequestType;
import com.avanzada.entity.State;
import com.avanzada.entity.User;
import com.avanzada.exception.InvalidStateTransitionException;
import com.avanzada.exception.RequestNotFoundException;
import com.avanzada.repository.ChannelRepository;
import com.avanzada.repository.HistoryEntryRepository;
import com.avanzada.repository.RequestRepository;
import com.avanzada.repository.RequestTypeRepository;
import com.avanzada.repository.StateRepository;
import com.avanzada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Implements request lifecycle (state machine), prioritization, assignment, and history (RF-03, RF-04, RF-05, RF-06, RF-08).
 */
@Service
@RequiredArgsConstructor
public class RequestLifecycleServiceImpl implements RequestLifecycleService {

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
    private final RequestPriorityRuleEngine requestPriorityRuleEngine;

    @Override
    @Transactional
    public Request createRequest(String description, Long requestTypeId, Long channelId, Long requestedById, Long performedById, String registeredAt, String idempotencyKey) {
        String normalizedDescription = normalizeDescription(description);
        RequestType requestType = requestTypeRepository.findById(requestTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Request type not found: " + requestTypeId));
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        User requestedBy = userRepository.findById(requestedById)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requestedById));
        State state = stateRepository.findByCode(REGISTRADA)
                .orElseThrow(() -> new IllegalStateException("State REGISTRADA not found; ensure reference data is loaded"));
        String normalizedIdempotencyKey = normalizeNullable(idempotencyKey);
        if (normalizedIdempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        Request existing = requestRepository.findByRequestedBy_IdAndIdempotencyKey(requestedById, normalizedIdempotencyKey)
                .orElse(null);
        if (existing != null) {
            validateReplayPayload(existing, normalizedDescription, requestTypeId, channelId);
            return existing;
        }

        Instant now = parseInstant(registeredAt);
        if (now == null) {
            now = Instant.now();
        }

        Request request = Request.builder()
                .description(normalizedDescription)
                .registeredAt(now)
                .requestType(requestType)
                .channel(channel)
                .state(state)
                .requestedBy(requestedBy)
                .idempotencyKey(normalizedIdempotencyKey)
                .build();

        try {
            request = requestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException ex) {
            Request replay = requestRepository.findByRequestedBy_IdAndIdempotencyKey(requestedById, normalizedIdempotencyKey)
                    .orElse(null);
            if (replay != null) {
                validateReplayPayload(replay, normalizedDescription, requestTypeId, channelId);
                return replay;
            }
            throw ex;
        }

        Long actorId = performedById != null ? performedById : requestedById;
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + actorId));
        appendHistory(request, "REGISTERED", actor, "Request registered");
        return request;
    }

    @Override
    public List<Request> listByFilters(String state, Long requestType, String priority, Long assignedTo, Long requestedById) {
        Long stateId = resolveStateIdOrThrow(state);
        Priority priorityEnum = resolvePriorityOrThrow(priority);
        return requestRepository.findByFilters(stateId, requestType, priorityEnum, assignedTo, requestedById);
    }

    @Override
    public List<HistoryEntry> listHistory(Long requestId) {
        findRequestOrThrow(requestId);
        return historyEntryRepository.findByRequest_IdOrderByOccurredAtDesc(requestId);
    }

    /**
     * Classify request and derive priority from backend rules; transition REGISTRADA -> CLASIFICADA (RF-02, RF-03, RF-04).
     */
    @Override
    @Transactional
    public Request classify(Long requestId, Long requestTypeId, String priority, String priorityJustification, Long userId) {
        Request request = findRequestOrThrow(requestId);
        RequestType requestType = requestTypeRepository.findById(requestTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Request type not found: " + requestTypeId));
        RequestPriorityRuleEngine.PriorityDecision decision = requestPriorityRuleEngine.evaluate(request, requestType);

        if (isClassificationReplay(request, requestTypeId, decision)) {
            return request;
        }
        requireState(request, REGISTRADA, "classify");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        State clasificada = stateRepository.findByCode(CLASIFICADA)
                .orElseThrow(() -> new IllegalStateException("State CLASIFICADA not found"));

        request.setRequestType(requestType);
        request.setPriority(decision.priority());
        request.setPriorityJustification(decision.justification());
        request.setState(clasificada);
        try {
            request = requestRepository.saveAndFlush(request);
        } catch (OptimisticLockingFailureException ex) {
            Request current = findRequestOrThrow(requestId);
            if (isClassificationReplay(current, requestTypeId, decision)) {
                return current;
            }
            throw ex;
        }
        appendHistory(request, "CLASSIFIED", user, "Type: " + requestType.getCode() + ". " + decision.justification());
        return request;
    }

    /**
     * Assign responsible; transition CLASIFICADA -> EN_ATENCION (RF-05, RF-04). Validates assignee is active.
     */
    @Override
    @Transactional
    public Request assign(Long requestId, Long assignedToId, Long userId) {
        Request request = findRequestOrThrow(requestId);
        if (isAssignmentReplay(request, assignedToId)) {
            return request;
        }
        requireState(request, CLASIFICADA, "assign");

        User assignee = userRepository.findById(assignedToId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + assignedToId));
        if (Boolean.FALSE.equals(assignee.getActive())) {
            throw new IllegalArgumentException("Cannot assign to inactive user: " + assignedToId);
        }
        if (!isAuthorizedResponsible(assignee)) {
            throw new IllegalArgumentException("Cannot assign to user without authorized role: " + assignedToId);
        }
        User performingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        State enAtencion = stateRepository.findByCode(EN_ATENCION)
                .orElseThrow(() -> new IllegalStateException("State EN_ATENCION not found"));

        request.setAssignedTo(assignee);
        request.setState(enAtencion);
        try {
            request = requestRepository.saveAndFlush(request);
        } catch (OptimisticLockingFailureException ex) {
            Request current = findRequestOrThrow(requestId);
            if (isAssignmentReplay(current, assignedToId)) {
                return current;
            }
            throw ex;
        }
        appendHistory(request, "ASSIGNED", performingUser, "Assigned to " + assignee.getName() + " (" + assignee.getIdentifier() + ")");
        return request;
    }

    /**
     * Mark as attended; transition EN_ATENCION -> ATENDIDA (RF-04).
     */
    @Override
    @Transactional
    public Request attend(Long requestId, Long userId, String observations) {
        Request request = findRequestOrThrow(requestId);
        String normalizedObservations = normalizeNullable(observations);
        if (isAttendReplay(request, normalizedObservations)) {
            return request;
        }
        requireState(request, EN_ATENCION, "attend");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        State atendida = stateRepository.findByCode(ATENDIDA)
                .orElseThrow(() -> new IllegalStateException("State ATENDIDA not found"));

        request.setState(atendida);
        try {
            request = requestRepository.saveAndFlush(request);
        } catch (OptimisticLockingFailureException ex) {
            Request current = findRequestOrThrow(requestId);
            if (isAttendReplay(current, normalizedObservations)) {
                return current;
            }
            throw ex;
        }
        appendHistory(request, "ATTENDED", user, normalizedObservations);
        return request;
    }

    /**
     * Close request; transition ATENDIDA -> CERRADA only, with required closure observation (RF-08).
     */
    @Override
    @Transactional
    public Request close(Long requestId, String closureObservation, Long userId) {
        Request request = findRequestOrThrow(requestId);
        String normalizedClosureObservation = normalizeNullable(closureObservation);
        if (isCloseReplay(request, normalizedClosureObservation)) {
            return request;
        }
        requireState(request, ATENDIDA, "close");

        if (normalizedClosureObservation == null) {
            throw new IllegalArgumentException("Closure observation is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        State cerrada = stateRepository.findByCode(CERRADA)
                .orElseThrow(() -> new IllegalStateException("State CERRADA not found"));

        request.setClosureObservation(normalizedClosureObservation);
        request.setState(cerrada);
        try {
            request = requestRepository.saveAndFlush(request);
        } catch (OptimisticLockingFailureException ex) {
            Request current = findRequestOrThrow(requestId);
            if (isCloseReplay(current, normalizedClosureObservation)) {
                return current;
            }
            throw ex;
        }
        appendHistory(request, "CLOSED", user, normalizedClosureObservation);
        return request;
    }

    @Override
    public Priority suggestPriorityByRequestType(RequestType requestType) {
        return requestPriorityRuleEngine.basePriorityFor(requestType);
    }

    @Override
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

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            throw new IllegalArgumentException("Description is required");
        }
        String normalizedDescription = description.trim();
        if (normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        return normalizedDescription;
    }

    private Long resolveStateIdOrThrow(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalizedState = value.trim().toUpperCase(Locale.ROOT);
        return stateRepository.findByCode(normalizedState)
                .map(State::getId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid state filter: " + value));
    }

    private static Priority resolvePriorityOrThrow(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority filter: " + value);
        }
    }

    private static boolean isAuthorizedResponsible(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole().trim();
        return "STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    private boolean isClassificationReplay(Request request, Long requestTypeId, RequestPriorityRuleEngine.PriorityDecision decision) {
        if (request.getState() == null || !CLASIFICADA.equals(request.getState().getCode())) {
            return false;
        }
        return request.getRequestType() != null
                && request.getRequestType().getId().equals(requestTypeId)
                && request.getPriority() == decision.priority()
                && Objects.equals(normalizeNullable(request.getPriorityJustification()), normalizeNullable(decision.justification()));
    }

    private boolean isAssignmentReplay(Request request, Long assignedToId) {
        if (request.getState() == null || !EN_ATENCION.equals(request.getState().getCode())) {
            return false;
        }
        return request.getAssignedTo() != null && request.getAssignedTo().getId().equals(assignedToId);
    }

    private boolean isAttendReplay(Request request, String observations) {
        if (request.getState() == null || !ATENDIDA.equals(request.getState().getCode())) {
            return false;
        }
        return historyEntryRepository.findFirstByRequest_IdAndActionOrderByOccurredAtDesc(request.getId(), "ATTENDED")
                .map(entry -> Objects.equals(normalizeNullable(entry.getObservations()), observations))
                .orElse(observations == null);
    }

    private boolean isCloseReplay(Request request, String closureObservation) {
        if (request.getState() == null || !CERRADA.equals(request.getState().getCode())) {
            return false;
        }
        return Objects.equals(normalizeNullable(request.getClosureObservation()), closureObservation);
    }

    private void validateReplayPayload(Request existing, String description, Long requestTypeId, Long channelId) {
        boolean samePayload = Objects.equals(normalizeNullable(existing.getDescription()), normalizeNullable(description))
                && existing.getRequestType() != null
                && existing.getRequestType().getId().equals(requestTypeId)
                && existing.getChannel() != null
                && existing.getChannel().getId().equals(channelId);
        if (!samePayload) {
            throw new IllegalArgumentException("Idempotency-Key was already used with a different request payload");
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
