package com.avanzada.service;

import com.avanzada.entity.*;
import com.avanzada.exception.InvalidStateTransitionException;
import com.avanzada.exception.RequestNotFoundException;
import com.avanzada.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequestLifecycleServiceTest {

    @Autowired
    private RequestLifecycleService lifecycleService;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private RequestTypeRepository requestTypeRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HistoryEntryRepository historyEntryRepository;

    private User requester;
    private User assignee;
    private User otherUser;

    @BeforeEach
    void setUp() {
        ensureStates();
        requester = userRepository.save(User.builder()
                .identifier("requester@test.com")
                .name("Requester")
                .active(true)
                .build());
        assignee = userRepository.save(User.builder()
                .identifier("assignee@test.com")
                .name("Assignee")
                .active(true)
                .build());
        otherUser = userRepository.save(User.builder()
                .identifier("other@test.com")
                .name("Other")
                .active(true)
                .build());
    }

    private void ensureStates() {
        String[][] states = {
                {"REGISTRADA", "Registrada", "1"},
                {"CLASIFICADA", "Clasificada", "2"},
                {"EN_ATENCION", "En atención", "3"},
                {"ATENDIDA", "Atendida", "4"},
                {"CERRADA", "Cerrada", "5"}
        };
        for (String[] s : states) {
            if (stateRepository.findByCode(s[0]).isEmpty()) {
                stateRepository.save(State.builder()
                        .code(s[0])
                        .name(s[1])
                        .displayOrder(Integer.parseInt(s[2]))
                        .build());
            }
        }
    }

    @Test
    void fullLifecycle_registersHistoryAtEachStep() {
        RequestType type = requestTypeRepository.save(RequestType.builder().code("REG_ASIG").name("Registro").build());
        Channel channel = channelRepository.save(Channel.builder().code("CSU").name("CSU").build());

        Request created = lifecycleService.createRequest("Need to register", type.getId(), channel.getId(), requester.getId(), null);
        assertThat(created.getState().getCode()).isEqualTo("REGISTRADA");
        assertThat(historyEntries(created.getId())).hasSize(1);
        assertThat(historyEntries(created.getId()).get(0).getAction()).isEqualTo("REGISTERED");

        Request classified = lifecycleService.classify(created.getId(), type.getId(), Priority.HIGH, "Urgent", otherUser.getId());
        assertThat(classified.getState().getCode()).isEqualTo("CLASIFICADA");
        assertThat(classified.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(classified.getPriorityJustification()).isEqualTo("Urgent");
        assertThat(historyEntries(created.getId())).hasSize(2);
        assertThat(historyEntries(created.getId()).get(0).getAction()).isEqualTo("CLASSIFIED");

        Request assigned = lifecycleService.assign(classified.getId(), assignee.getId(), otherUser.getId());
        assertThat(assigned.getState().getCode()).isEqualTo("EN_ATENCION");
        assertThat(assigned.getAssignedTo().getId()).isEqualTo(assignee.getId());
        assertThat(historyEntries(created.getId())).hasSize(3);
        assertThat(historyEntries(created.getId()).get(0).getAction()).isEqualTo("ASSIGNED");

        Request attended = lifecycleService.attend(assigned.getId(), assignee.getId(), "Resolved");
        assertThat(attended.getState().getCode()).isEqualTo("ATENDIDA");
        assertThat(historyEntries(created.getId())).hasSize(4);
        assertThat(historyEntries(created.getId()).get(0).getAction()).isEqualTo("ATTENDED");

        Request closed = lifecycleService.close(attended.getId(), "Closed after verification", otherUser.getId());
        assertThat(closed.getState().getCode()).isEqualTo("CERRADA");
        assertThat(closed.getClosureObservation()).isEqualTo("Closed after verification");
        assertThat(historyEntries(created.getId())).hasSize(5);
        assertThat(historyEntries(created.getId()).get(0).getAction()).isEqualTo("CLOSED");
    }

    @Test
    void classify_fromNonRegistrada_throwsInvalidStateTransition() {
        RequestType type = requestTypeRepository.save(RequestType.builder().code("T").name("T").build());
        Channel channel = channelRepository.save(Channel.builder().code("C").name("C").build());
        Request created = lifecycleService.createRequest("Desc", type.getId(), channel.getId(), requester.getId(), null);
        lifecycleService.classify(created.getId(), type.getId(), Priority.MEDIUM, null, otherUser.getId());

        assertThatThrownBy(() -> lifecycleService.classify(created.getId(), type.getId(), Priority.LOW, null, otherUser.getId()))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("expected REGISTRADA");
    }

    @Test
    void assign_toInactiveUser_throws() {
        ensureStates();
        User inactive = userRepository.save(User.builder().identifier("inactive@test.com").name("Inactive").active(false).build());
        RequestType type = requestTypeRepository.save(RequestType.builder().code("T").name("T").build());
        Channel channel = channelRepository.save(Channel.builder().code("C").name("C").build());
        Request created = lifecycleService.createRequest("Desc", type.getId(), channel.getId(), requester.getId(), null);
        lifecycleService.classify(created.getId(), type.getId(), Priority.MEDIUM, null, otherUser.getId());

        assertThatThrownBy(() -> lifecycleService.assign(created.getId(), inactive.getId(), otherUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void close_withoutObservation_throws() {
        RequestType type = requestTypeRepository.save(RequestType.builder().code("T").name("T").build());
        Channel channel = channelRepository.save(Channel.builder().code("C").name("C").build());
        Request created = lifecycleService.createRequest("Desc", type.getId(), channel.getId(), requester.getId(), null);
        lifecycleService.classify(created.getId(), type.getId(), Priority.MEDIUM, null, otherUser.getId());
        lifecycleService.assign(created.getId(), assignee.getId(), otherUser.getId());
        lifecycleService.attend(created.getId(), assignee.getId(), null);

        assertThatThrownBy(() -> lifecycleService.close(created.getId(), "", otherUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Closure observation is required");
        assertThatThrownBy(() -> lifecycleService.close(created.getId(), null, otherUser.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void close_fromNonAtendida_throwsInvalidStateTransition() {
        RequestType type = requestTypeRepository.save(RequestType.builder().code("T").name("T").build());
        Channel channel = channelRepository.save(Channel.builder().code("C").name("C").build());
        Request created = lifecycleService.createRequest("Desc", type.getId(), channel.getId(), requester.getId(), null);

        assertThatThrownBy(() -> lifecycleService.close(created.getId(), "obs", otherUser.getId()))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("expected ATENDIDA");
    }

    @Test
    void closedRequest_cannotBeModified() {
        RequestType type = requestTypeRepository.save(RequestType.builder().code("T").name("T").build());
        Channel channel = channelRepository.save(Channel.builder().code("C").name("C").build());
        Request created = lifecycleService.createRequest("Desc", type.getId(), channel.getId(), requester.getId(), null);
        lifecycleService.classify(created.getId(), type.getId(), Priority.MEDIUM, null, otherUser.getId());
        lifecycleService.assign(created.getId(), assignee.getId(), otherUser.getId());
        lifecycleService.attend(created.getId(), assignee.getId(), null);
        lifecycleService.close(created.getId(), "Done", otherUser.getId());

        assertThatThrownBy(() -> lifecycleService.attend(created.getId(), assignee.getId(), null))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void suggestPriorityByRequestType_returnsExpectedPriority() {
        assertThat(lifecycleService.suggestPriorityByRequestType(
                RequestType.builder().code("HOMOLOG").build())).isEqualTo(Priority.HIGH);
        assertThat(lifecycleService.suggestPriorityByRequestType(
                RequestType.builder().code("CONSULTA").build())).isEqualTo(Priority.LOW);
        assertThat(lifecycleService.suggestPriorityByRequestType(
                RequestType.builder().code("REG_ASIG").build())).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void findRequestOrThrow_throwsWhenNotFound() {
        assertThatThrownBy(() -> lifecycleService.findRequestOrThrow(999999L))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessageContaining("999999");
    }

    private List<HistoryEntry> historyEntries(Long requestId) {
        return historyEntryRepository.findByRequest_IdOrderByOccurredAtDesc(requestId);
    }
}
