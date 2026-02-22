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
public class ClassifyRequestDto {

    @NotNull(message = "requestTypeId is required")
    private Long requestTypeId;

    @NotNull(message = "priority is required")
    private String priority; // LOW, MEDIUM, HIGH

    private String priorityJustification;
}
