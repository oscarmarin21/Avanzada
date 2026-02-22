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
public class HistoryEntryDto {

    private Long id;
    private Long requestId;
    private String occurredAt;
    private String action;
    private Long userId;
    private String userIdentifier;
    private String userName;
    private String observations;
}
