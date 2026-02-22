package com.avanzada.dto;

import com.avanzada.entity.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class RequestMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public RequestResponseDto toRequestResponseDto(Request r) {
        if (r == null) return null;
        return RequestResponseDto.builder()
                .id(r.getId())
                .description(r.getDescription())
                .registeredAt(format(r.getRegisteredAt()))
                .requestTypeId(r.getRequestType() != null ? r.getRequestType().getId() : null)
                .requestTypeCode(r.getRequestType() != null ? r.getRequestType().getCode() : null)
                .requestTypeName(r.getRequestType() != null ? r.getRequestType().getName() : null)
                .channelId(r.getChannel() != null ? r.getChannel().getId() : null)
                .channelCode(r.getChannel() != null ? r.getChannel().getCode() : null)
                .channelName(r.getChannel() != null ? r.getChannel().getName() : null)
                .stateId(r.getState() != null ? r.getState().getId() : null)
                .stateCode(r.getState() != null ? r.getState().getCode() : null)
                .stateName(r.getState() != null ? r.getState().getName() : null)
                .priority(r.getPriority() != null ? r.getPriority().name() : null)
                .priorityJustification(r.getPriorityJustification())
                .requestedById(r.getRequestedBy() != null ? r.getRequestedBy().getId() : null)
                .requestedByIdentifier(r.getRequestedBy() != null ? r.getRequestedBy().getIdentifier() : null)
                .requestedByName(r.getRequestedBy() != null ? r.getRequestedBy().getName() : null)
                .assignedToId(r.getAssignedTo() != null ? r.getAssignedTo().getId() : null)
                .assignedToIdentifier(r.getAssignedTo() != null ? r.getAssignedTo().getIdentifier() : null)
                .assignedToName(r.getAssignedTo() != null ? r.getAssignedTo().getName() : null)
                .closureObservation(r.getClosureObservation())
                .createdAt(format(r.getCreatedAt()))
                .updatedAt(format(r.getUpdatedAt()))
                .build();
    }

    public HistoryEntryDto toHistoryEntryDto(HistoryEntry e) {
        if (e == null) return null;
        return HistoryEntryDto.builder()
                .id(e.getId())
                .requestId(e.getRequest() != null ? e.getRequest().getId() : null)
                .occurredAt(format(e.getOccurredAt()))
                .action(e.getAction())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .userIdentifier(e.getUser() != null ? e.getUser().getIdentifier() : null)
                .userName(e.getUser() != null ? e.getUser().getName() : null)
                .observations(e.getObservations())
                .build();
    }

    public RequestTypeDto toRequestTypeDto(RequestType e) {
        if (e == null) return null;
        return RequestTypeDto.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }

    public ChannelDto toChannelDto(Channel e) {
        if (e == null) return null;
        return ChannelDto.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .build();
    }

    public StateDto toStateDto(State e) {
        if (e == null) return null;
        return StateDto.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .order(e.getDisplayOrder())
                .build();
    }

    public UserDto toUserDto(User e) {
        if (e == null) return null;
        return UserDto.builder()
                .id(e.getId())
                .identifier(e.getIdentifier())
                .name(e.getName())
                .active(e.getActive())
                .build();
    }

    private static String format(Instant instant) {
        return instant != null ? ISO.format(instant) : null;
    }
}
