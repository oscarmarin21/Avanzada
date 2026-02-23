package com.avanzada.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Indicates whether AI features (suggest, summary) are available.
 * Frontend can hide AI buttons when available is false.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStatusDto {

    private boolean available;
}
