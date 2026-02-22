package com.avanzada.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestResponseDto {

    private Long id;
    private String description;
    private String registeredAt;
    private Long requestTypeId;
    private String requestTypeCode;
    private String requestTypeName;
    private Long channelId;
    private String channelCode;
    private String channelName;
    private Long stateId;
    private String stateCode;
    private String stateName;
    private String priority;
    private String priorityJustification;
    private Long requestedById;
    private String requestedByIdentifier;
    private String requestedByName;
    private Long assignedToId;
    private String assignedToIdentifier;
    private String assignedToName;
    private String closureObservation;
    private String createdAt;
    private String updatedAt;
}
