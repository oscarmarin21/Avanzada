package com.avanzada.repository;

import com.avanzada.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HistoryEntryRepositoryTest {

    @Autowired
    private HistoryEntryRepository historyEntryRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RequestTypeRepository requestTypeRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Request request;
    private User user;

    @BeforeEach
    void setUp() {
        RequestType requestType = requestTypeRepository.save(RequestType.builder()
                .code("REG_ASIG")
                .name("Registro")
                .build());
        Channel channel = channelRepository.save(Channel.builder().code("CSU").name("CSU").build());
        State state = stateRepository.save(State.builder().code("REGISTRADA").name("Registrada").displayOrder(1).build());
        user = userRepository.save(User.builder().identifier("u@test.com").name("User").active(true).build());
        request = requestRepository.save(Request.builder()
                .description("Test")
                .registeredAt(Instant.now())
                .requestType(requestType)
                .channel(channel)
                .state(state)
                .requestedBy(user)
                .build());
        entityManager.flush();
    }

    @Test
    void saveHistoryEntry_andFindByRequestId() {
        HistoryEntry entry = historyEntryRepository.save(HistoryEntry.builder()
                .request(request)
                .occurredAt(Instant.now())
                .action("REGISTERED")
                .user(user)
                .observations("Created")
                .build());
        entityManager.flush();
        entityManager.clear();

        List<HistoryEntry> entries = historyEntryRepository.findByRequest_IdOrderByOccurredAtDesc(request.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getAction()).isEqualTo("REGISTERED");
        assertThat(entries.get(0).getObservations()).isEqualTo("Created");
    }
}
