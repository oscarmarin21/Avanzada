package com.avanzada.service;

import com.avanzada.entity.Priority;
import com.avanzada.entity.Request;
import com.avanzada.entity.RequestType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestPriorityRuleEngineTest {

    private final RequestPriorityRuleEngine ruleEngine = new RequestPriorityRuleEngine();

    @Test
    void evaluate_usesRequestTypeAsBaseRule() {
        assertThat(ruleEngine.evaluate(request("Consulta general"), requestType("CONSULTA")).priority())
                .isEqualTo(Priority.LOW);
        assertThat(ruleEngine.evaluate(request("Solicitud de homologacion"), requestType("HOMOLOG")).priority())
                .isEqualTo(Priority.HIGH);
        assertThat(ruleEngine.evaluate(request("Registro de asignatura"), requestType("REG_ASIG")).priority())
                .isEqualTo(Priority.MEDIUM);
    }

    @Test
    void evaluate_promotesPriorityWhenDescriptionContainsAcademicAndDeadlineSignals() {
        RequestPriorityRuleEngine.PriorityDecision decision = ruleEngine.evaluate(
                request("Consulta sobre matricula con fecha limite y avance academico"),
                requestType("CONSULTA"));

        assertThat(decision.priority()).isEqualTo(Priority.HIGH);
        assertThat(decision.justification()).contains("request type CONSULTA maps to LOW");
        assertThat(decision.justification()).contains("deadline signal");
        assertThat(decision.justification()).contains("academic impact signal");
        assertThat(decision.justification()).contains("final priority HIGH");
    }

    private static Request request(String description) {
        return Request.builder().description(description).build();
    }

    private static RequestType requestType(String code) {
        return RequestType.builder().code(code).build();
    }
}
