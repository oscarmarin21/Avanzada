package com.avanzada.service;

import com.avanzada.entity.Priority;
import com.avanzada.entity.Request;
import com.avanzada.entity.RequestType;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backend rule engine that derives request priority from request type and contextual signals.
 */
@Component
public class RequestPriorityRuleEngine {

    private static final List<String> DEADLINE_KEYWORDS = List.of(
            "fecha limite",
            "deadline",
            "plazo",
            "vencimiento",
            "cierre de matricula",
            "cierre de inscripcion"
    );

    private static final List<String> ACADEMIC_IMPACT_KEYWORDS = List.of(
            "homolog",
            "cupo",
            "matricula",
            "inscripcion",
            "plan de estudios",
            "semestre"
    );

    private static final List<String> URGENCY_KEYWORDS = List.of(
            "urgente",
            "inmediato",
            "lo antes posible",
            "hoy",
            "manana"
    );

    public PriorityDecision evaluate(Request request, RequestType requestType) {
        Priority priority = basePriorityFor(requestType);
        String typeCode = requestType != null && requestType.getCode() != null
                ? requestType.getCode().trim().toUpperCase(Locale.ROOT)
                : "UNKNOWN";
        String description = normalize(request != null ? request.getDescription() : null);

        List<String> reasons = new ArrayList<>();
        reasons.add("request type " + typeCode + " maps to " + priority);

        priority = promoteIfSignalMatches(priority, description, DEADLINE_KEYWORDS, "deadline signal", reasons);
        priority = promoteIfSignalMatches(priority, description, ACADEMIC_IMPACT_KEYWORDS, "academic impact signal", reasons);
        priority = promoteIfSignalMatches(priority, description, URGENCY_KEYWORDS, "urgency signal", reasons);

        reasons.add("final priority " + priority);
        return new PriorityDecision(priority, String.join("; ", reasons) + ".");
    }

    public Priority basePriorityFor(RequestType requestType) {
        if (requestType == null || requestType.getCode() == null) {
            return Priority.MEDIUM;
        }
        return switch (requestType.getCode().trim().toUpperCase(Locale.ROOT)) {
            case "HOMOLOG", "CUPOS" -> Priority.HIGH;
            case "CONSULTA" -> Priority.LOW;
            case "REG_ASIG", "CANCEL" -> Priority.MEDIUM;
            default -> Priority.MEDIUM;
        };
    }

    private static Priority promoteIfSignalMatches(Priority current,
                                                    String normalizedDescription,
                                                    List<String> keywords,
                                                    String signalLabel,
                                                    List<String> reasons) {
        List<String> matches = findMatches(normalizedDescription, keywords);
        if (matches.isEmpty()) {
            return current;
        }

        Priority promoted = promote(current);
        if (promoted == current) {
            reasons.add(signalLabel + " via [" + String.join(", ", matches) + "] keeps priority at " + current);
        } else {
            reasons.add(signalLabel + " via [" + String.join(", ", matches) + "] raises priority from " + current + " to " + promoted);
        }
        return promoted;
    }

    private static Priority promote(Priority current) {
        if (current == null) {
            return Priority.MEDIUM;
        }
        return switch (current) {
            case LOW -> Priority.MEDIUM;
            case MEDIUM -> Priority.HIGH;
            case HIGH -> Priority.HIGH;
        };
    }

    private static List<String> findMatches(String normalizedDescription, List<String> keywords) {
        if (normalizedDescription == null || normalizedDescription.isBlank()) {
            return List.of();
        }
        List<String> matches = new ArrayList<>();
        for (String keyword : keywords) {
            if (normalizedDescription.contains(keyword)) {
                matches.add(keyword);
            }
        }
        return matches;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    public record PriorityDecision(Priority priority, String justification) {
    }
}
