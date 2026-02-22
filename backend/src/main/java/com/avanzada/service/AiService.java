package com.avanzada.service;

import com.avanzada.dto.SuggestResponseDto;
import com.avanzada.dto.SummaryResponseDto;
import com.avanzada.entity.Request;

/**
 * Optional AI integration (RF-09, RF-10, RF-11). Best-effort; when disabled or failing, fallbacks are returned.
 */
public interface AiService {

    SuggestResponseDto suggestTypeAndPriority(String description);

    SummaryResponseDto generateSummary(Request request);
}
