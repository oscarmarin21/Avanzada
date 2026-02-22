package com.avanzada.service;

import com.avanzada.config.AiProperties;
import com.avanzada.dto.SuggestResponseDto;
import com.avanzada.dto.SummaryResponseDto;
import com.avanzada.entity.HistoryEntry;
import com.avanzada.entity.Request;
import com.avanzada.repository.HistoryEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional IA integration (RF-09, RF-10, RF-11). All methods are best-effort;
 * when IA is disabled or fails, fallbacks are returned so core flows never depend on IA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private static final String[] REQUEST_TYPE_CODES = {"REG_ASIG", "HOMOLOG", "CANCEL", "CUPOS", "CONSULTA"};
    private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH"};
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final AiProperties aiProperties;
    private final HistoryEntryRepository historyEntryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Suggests request type and priority from description (RF-10). Client must confirm before applying.
     */
    public SuggestResponseDto suggestTypeAndPriority(String description) {
        if (!aiProperties.isConfigured()) {
            return SuggestResponseDto.builder()
                    .available(false)
                    .message("AI suggestion is disabled or not configured.")
                    .build();
        }
        String prompt = buildSuggestionPrompt(description);
        try {
            String content = callLlm(prompt, 150);
            if (content == null || content.isBlank()) {
                return SuggestResponseDto.builder().available(false).message("No suggestion returned.").build();
            }
            return parseSuggestionResponse(content);
        } catch (Exception e) {
            log.warn("AI suggestion failed: {}", e.getMessage());
            return SuggestResponseDto.builder()
                    .available(false)
                    .message("Suggestion temporarily unavailable: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Generates a textual summary of the request and its history (RF-09). Returns fallback when IA unavailable.
     */
    public SummaryResponseDto generateSummary(Request request) {
        List<HistoryEntry> history = historyEntryRepository.findByRequest_IdOrderByOccurredAtDesc(request.getId());
        if (!aiProperties.isConfigured()) {
            return SummaryResponseDto.builder()
                    .summary(buildFallbackSummary(request, history))
                    .fromAi(false)
                    .build();
        }
        String context = buildSummaryContext(request, history);
        try {
            String prompt = "Summarize in 2-4 short sentences this academic request and its lifecycle. Be concise.\n\n" + context;
            String summary = callLlm(prompt, 300);
            if (summary != null && !summary.isBlank()) {
                return SummaryResponseDto.builder().summary(summary.trim()).fromAi(true).build();
            }
        } catch (Exception e) {
            log.warn("AI summary failed for request {}: {}", request.getId(), e.getMessage());
        }
        return SummaryResponseDto.builder()
                .summary(buildFallbackSummary(request, history))
                .fromAi(false)
                .build();
    }

    private String callLlm(String userMessage, int maxTokens) throws Exception {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = aiProperties.getTimeoutSeconds() * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        RestTemplate rest = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(Map.of("role", "user", "content", userMessage)),
                "max_tokens", maxTokens
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = rest.exchange(
                aiProperties.getEndpoint(),
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getBody() == null) return null;
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) return null;
        JsonNode message = choices.get(0).path("message");
        JsonNode content = message.path("content");
        return content.isTextual() ? content.asText() : null;
    }

    private String buildSuggestionPrompt(String description) {
        return "Given this academic request description, suggest exactly one request type code and one priority. "
                + "Request type codes (choose one): " + String.join(", ", REQUEST_TYPE_CODES) + ". "
                + "Priority (choose one): " + String.join(", ", PRIORITIES) + ". "
                + "Reply with only a single line in this exact format: requestTypeCode=CODE priority=PRIORITY. "
                + "Example: requestTypeCode=HOMOLOG priority=HIGH\n\nDescription:\n" + description;
    }

    private SuggestResponseDto parseSuggestionResponse(String content) {
        String requestTypeCode = null;
        String priority = null;
        Pattern typePattern = Pattern.compile("requestTypeCode\\s*=\\s*([A-Z_]+)", Pattern.CASE_INSENSITIVE);
        Pattern priorityPattern = Pattern.compile("priority\\s*=\\s*(" + String.join("|", PRIORITIES) + ")", Pattern.CASE_INSENSITIVE);
        Matcher typeMatcher = typePattern.matcher(content);
        Matcher priorityMatcher = priorityPattern.matcher(content);
        if (typeMatcher.find()) {
            String code = typeMatcher.group(1).toUpperCase();
            for (String valid : REQUEST_TYPE_CODES) {
                if (valid.equals(code)) {
                    requestTypeCode = code;
                    break;
                }
            }
        }
        if (priorityMatcher.find()) {
            priority = priorityMatcher.group(1).toUpperCase();
        }
        return SuggestResponseDto.builder()
                .suggestedRequestTypeCode(requestTypeCode)
                .suggestedPriority(priority)
                .available(true)
                .build();
    }

    private String buildSummaryContext(Request request, List<HistoryEntry> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request #").append(request.getId()).append(": ").append(request.getDescription()).append("\n");
        sb.append("State: ").append(request.getState() != null ? request.getState().getName() : "?").append("\n");
        sb.append("Priority: ").append(request.getPriority() != null ? request.getPriority() : "not set").append("\n");
        if (!history.isEmpty()) {
            sb.append("History:\n");
            for (int i = 0; i < Math.min(history.size(), 10); i++) {
                HistoryEntry e = history.get(i);
                sb.append("- ").append(ISO.format(e.getOccurredAt())).append(" ").append(e.getAction());
                if (e.getObservations() != null && !e.getObservations().isBlank()) {
                    sb.append(": ").append(e.getObservations());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String buildFallbackSummary(Request request, List<HistoryEntry> history) {
        String stateName = request.getState() != null ? request.getState().getName() : "Unknown";
        return String.format("Request #%d – %s. Current state: %s. %d history entries.",
                request.getId(),
                request.getDescription().length() > 80 ? request.getDescription().substring(0, 80) + "…" : request.getDescription(),
                stateName,
                history.size());
    }
}
