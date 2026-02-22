package com.avanzada.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Suggestion from IA (RF-10). Client must confirm or adjust before applying; never auto-applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuggestResponseDto {

    private String suggestedRequestTypeCode;
    private String suggestedPriority;
    private Boolean available; // false when IA is unavailable (RF-11)
    private String message;    // optional message when unavailable or error
}
