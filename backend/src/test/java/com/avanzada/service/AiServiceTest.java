package com.avanzada.service;

import com.avanzada.config.AiProperties;
import com.avanzada.dto.SuggestResponseDto;
import com.avanzada.dto.SummaryResponseDto;
import com.avanzada.entity.*;
import com.avanzada.repository.HistoryEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private HistoryEntryRepository historyEntryRepository;

    @Autowired
    private com.avanzada.repository.RequestRepository requestRepository;

    @Autowired
    private com.avanzada.repository.RequestTypeRepository requestTypeRepository;

    @Autowired
    private com.avanzada.repository.ChannelRepository channelRepository;

    @Autowired
    private com.avanzada.repository.StateRepository stateRepository;

    @Autowired
    private com.avanzada.repository.UserRepository userRepository;

    @BeforeEach
    void setUp() {
        aiProperties.setEnabled(false);
        aiProperties.setApiKey("");
    }

    @Test
    void suggest_whenDisabled_returnsAvailableFalse() {
        SuggestResponseDto response = aiService.suggestTypeAndPriority("Need to homologate a course");
        assertThat(response.getAvailable()).isFalse();
        assertThat(response.getMessage()).isNotBlank();
        assertThat(response.getSuggestedRequestTypeCode()).isNull();
        assertThat(response.getSuggestedPriority()).isNull();
    }

    @Test
    void generateSummary_whenDisabled_returnsFallbackSummary() {
        RequestType type = requestTypeRepository.save(RequestType.builder().code("T").name("T").build());
        Channel channel = channelRepository.save(Channel.builder().code("C").name("C").build());
        State state = stateRepository.save(State.builder().code("REGISTRADA").name("Registrada").displayOrder(1).build());
        User user = userRepository.save(User.builder().identifier("u@t.com").name("U").active(true).build());
        Request request = requestRepository.save(Request.builder()
                .description("Test request for summary")
                .registeredAt(java.time.Instant.now())
                .requestType(type)
                .channel(channel)
                .state(state)
                .requestedBy(user)
                .build());

        SummaryResponseDto response = aiService.generateSummary(request);
        assertThat(response.getFromAi()).isFalse();
        assertThat(response.getSummary()).contains("Request #" + request.getId());
        assertThat(response.getSummary()).contains("Registrada");
    }
}
