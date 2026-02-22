package com.avanzada.config;

import com.avanzada.entity.Channel;
import com.avanzada.entity.RequestType;
import com.avanzada.entity.State;
import com.avanzada.entity.User;
import com.avanzada.repository.ChannelRepository;
import com.avanzada.repository.RequestTypeRepository;
import com.avanzada.repository.StateRepository;
import com.avanzada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds reference data and an initial admin user if missing.
 * Idempotent: only inserts when no record exists for the same code/identifier.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final StateRepository stateRepository;
    private final ChannelRepository channelRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void run() {
        ensureStates();
        ensureChannels();
        ensureRequestTypes();
        ensureAdminUser();
        log.info("Data initialization completed.");
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
                log.info("Created state: {}", s[0]);
            }
        }
    }

    private void ensureChannels() {
        String[][] channels = {
                {"CSU", "Centro de Servicios Universitarios"},
                {"EMAIL", "Correo electrónico"},
                {"SAC", "Sistema de Atención al Ciudadano"},
                {"PRESENCIAL", "Presencial"}
        };
        for (String[] c : channels) {
            if (channelRepository.findByCode(c[0]).isEmpty()) {
                channelRepository.save(Channel.builder().code(c[0]).name(c[1]).build());
                log.info("Created channel: {}", c[0]);
            }
        }
    }

    private void ensureRequestTypes() {
        String[][] types = {
                {"REG_ASIG", "Registro / Asignatura", "Trámites de registro o asignaturas"},
                {"HOMOLOG", "Homologación", "Homologación de materias"},
                {"CONSULTA", "Consulta académica", "Consultas generales"}
        };
        for (String[] t : types) {
            if (requestTypeRepository.findByCode(t[0]).isEmpty()) {
                requestTypeRepository.save(RequestType.builder()
                        .code(t[0])
                        .name(t[1])
                        .description(t[2])
                        .build());
                log.info("Created request type: {}", t[0]);
            }
        }
    }

    private void ensureAdminUser() {
        String identifier = "admin";
        String password = "admin123";
        var existing = userRepository.findByIdentifier(identifier);
        if (existing.isPresent()) {
            User admin = existing.get();
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setActive(true);
            admin.setRole("ADMIN");
            admin.setName("Administrator");
            userRepository.save(admin);
            log.info("Reset admin user: {} (password: {})", identifier, password);
            return;
        }
        User admin = User.builder()
                .identifier(identifier)
                .name("Administrator")
                .active(true)
                .role("ADMIN")
                .passwordHash(passwordEncoder.encode(password))
                .build();
        userRepository.save(admin);
        log.info("Created admin user: {} (password: {})", identifier, password);
    }
}
