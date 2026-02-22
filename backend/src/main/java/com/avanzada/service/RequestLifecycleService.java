package com.avanzada.service;

import com.avanzada.entity.HistoryEntry;
import com.avanzada.entity.Priority;
import com.avanzada.entity.Request;
import com.avanzada.entity.RequestType;

import java.util.List;

/**
 * Request lifecycle (state machine), prioritization, assignment, and history (RF-03–RF-08).
 */
public interface RequestLifecycleService {

    Request createRequest(String description, Long requestTypeId, Long channelId, Long requestedById, String registeredAt);

    List<Request> listByFilters(String state, Long requestType, String priority, Long assignedTo);

    Request findRequestOrThrow(Long requestId);

    Request classify(Long requestId, Long requestTypeId, String priority, String priorityJustification, Long userId);

    Request assign(Long requestId, Long assignedToId, Long userId);

    Request attend(Long requestId, Long userId, String observations);

    Request close(Long requestId, String closureObservation, Long userId);

    Priority suggestPriorityByRequestType(RequestType requestType);

    List<HistoryEntry> listHistory(Long requestId);
}
