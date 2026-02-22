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
class RequestRepositoryTest {


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

    private RequestType requestType;
    private Channel channel;
    private State stateRegistrada;
    private State stateClasificada;
    private User requester;
    private User assignee;

    @BeforeEach
    void setUp() {
        requestType = requestTypeRepository.save(RequestType.builder()
                .code("REG_ASIG")
                .name("Registro de asignaturas")
                .build());
        channel = channelRepository.save(Channel.builder()
                .code("CSU")
                .name("CSU")
                .build());
        stateRegistrada = stateRepository.save(State.builder()
                .code("REGISTRADA")
                .name("Registrada")
                .displayOrder(1)
                .build());
        stateClasificada = stateRepository.save(State.builder()
                .code("CLASIFICADA")
                .name("Clasificada")
                .displayOrder(2)
                .build());
        requester = userRepository.save(User.builder()
                .identifier("req@test.com")
                .name("Requester")
                .active(true)
                .build());
        assignee = userRepository.save(User.builder()
                .identifier("assignee@test.com")
                .name("Assignee")
                .active(true)
                .build());
    }

    @Test
    void saveRequest_andFindById() {
        Request request = Request.builder()
                .description("Test request")
                .registeredAt(Instant.now())
                .requestType(requestType)
                .channel(channel)
                .state(stateRegistrada)
                .requestedBy(requester)
                .build();
        request = requestRepository.save(request);
        entityManager.flush();
        entityManager.clear();

        Request found = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(found.getDescription()).isEqualTo("Test request");
        assertThat(found.getState().getCode()).isEqualTo("REGISTRADA");
    }

    @Test
    void findByFilters_returnsMatchingRequests() {
        Request r1 = requestRepository.save(Request.builder()
                .description("R1")
                .registeredAt(Instant.now())
                .requestType(requestType)
                .channel(channel)
                .state(stateRegistrada)
                .requestedBy(requester)
                .build());
        Request r2 = requestRepository.save(Request.builder()
                .description("R2")
                .registeredAt(Instant.now())
                .requestType(requestType)
                .channel(channel)
                .state(stateClasificada)
                .priority(Priority.HIGH)
                .requestedBy(requester)
                .assignedTo(assignee)
                .build());
        entityManager.flush();
        entityManager.clear();

        List<Request> all = requestRepository.findByFilters(null, null, null, null);
        assertThat(all).hasSize(2);

        List<Request> byState = requestRepository.findByFilters(stateRegistrada.getId(), null, null, null);
        assertThat(byState).hasSize(1);
        assertThat(byState.get(0).getState().getCode()).isEqualTo("REGISTRADA");

        List<Request> byPriority = requestRepository.findByFilters(null, null, Priority.HIGH, null);
        assertThat(byPriority).hasSize(1);
        assertThat(byPriority.get(0).getPriority()).isEqualTo(Priority.HIGH);

        List<Request> byAssignedTo = requestRepository.findByFilters(null, null, null, assignee.getId());
        assertThat(byAssignedTo).hasSize(1);
        assertThat(byAssignedTo.get(0).getAssignedTo().getId()).isEqualTo(assignee.getId());
    }
}
