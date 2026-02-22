package com.avanzada.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of a request (RF-09). When IA is unavailable, a fallback non-LLM summary is returned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryResponseDto {

    private String summary;
    private Boolean fromAi;   // false when fallback was used (IA unavailable)
}
