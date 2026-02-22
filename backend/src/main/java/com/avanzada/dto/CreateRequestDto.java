package com.avanzada.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequestDto {

    @NotNull(message = "description is required")
    private String description;

    @NotNull(message = "requestTypeId is required")
    private Long requestTypeId;

    @NotNull(message = "channelId is required")
    private Long channelId;

    @NotNull(message = "requestedById is required")
    private Long requestedById;

    private String registeredAt; // ISO-8601 optional; server sets now if null
}
