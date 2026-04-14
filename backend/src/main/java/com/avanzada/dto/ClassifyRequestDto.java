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

    /**
     * Legacy client hint kept for compatibility. The backend ignores it and derives the final priority.
     */
    private String priority;

    private String priorityJustification;
}
