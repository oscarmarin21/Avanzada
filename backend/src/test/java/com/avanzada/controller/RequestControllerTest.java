package com.avanzada.controller;

import com.avanzada.dto.CreateRequestDto;
import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.RequestResponseDto;
import com.avanzada.entity.Request;
import com.avanzada.security.AppUserDetails;
import com.avanzada.service.AiService;
import com.avanzada.service.RequestLifecycleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RequestLifecycleService lifecycleService;

    @Mock
    private RequestMapper mapper;

    @Mock
    private AiService aiService;

    @InjectMocks
    private RequestController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequest_adminRecordsAuthenticatedActorAndRequestedUserSeparately() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AppUserDetails(
                        7L,
                        "admin@test.com",
                        "secret",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))),
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        CreateRequestDto dto = CreateRequestDto.builder()
                .description("Need help")
                .requestTypeId(10L)
                .channelId(20L)
                .requestedById(99L)
                .build();

        when(lifecycleService.createRequest("Need help", 10L, 20L, 99L, 7L, null, "req-123"))
                .thenReturn(Request.builder().id(1L).build());
        when(mapper.toRequestResponseDto(any(Request.class)))
                .thenReturn(RequestResponseDto.builder().id(1L).build());

        var response = controller.createRequest(dto, "req-123");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(lifecycleService).createRequest("Need help", 10L, 20L, 99L, 7L, null, "req-123");
    }
}
