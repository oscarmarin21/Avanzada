package com.avanzada.config;

import com.avanzada.entity.*;
import com.avanzada.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Seeds reference data and sample data for all tables.
 * Idempotent: only inserts when no record exists for the same code/identifier; sample requests only when table is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final StateRepository stateRepository;
    private final ChannelRepository channelRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final HistoryEntryRepository historyEntryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void run() {
        ensureStates();
        ensureChannels();
        ensureRequestTypes();
        ensureUsers();
        ensureSampleRequests();
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
                {"CANCEL", "Cancelación de asignaturas", "Cancelación o retiro de asignaturas"},
                {"CUPOS", "Solicitud de cupos", "Solicitud de cupos en asignaturas"},
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

    private void ensureUsers() {
        List<UserSeed> seeds = List.of(
                new UserSeed("admin", "admin123", "Administrator", "ADMIN"),
                new UserSeed("staff", "staff123", "Staff User", "STAFF"),
                new UserSeed("student1", "student123", "Ana Student", "STUDENT"),
                new UserSeed("student2", "student123", "Bruno Student", "STUDENT"),
                new UserSeed("student3", "student123", "Carmen Student", "STUDENT"),
                new UserSeed("student4", "student123", "Diego Student", "STUDENT"),
                new UserSeed("student5", "student123", "Elena Student", "STUDENT")
        );
        for (UserSeed s : seeds) {
            var existing = userRepository.findByIdentifier(s.identifier);
            if (existing.isPresent()) {
                User u = existing.get();
                u.setPasswordHash(passwordEncoder.encode(s.password));
                u.setActive(true);
                u.setRole(s.role);
                u.setName(s.name);
                userRepository.save(u);
                log.info("Reset user: {} (password: {})", s.identifier, s.password);
            } else {
                User u = User.builder()
                        .identifier(s.identifier)
                        .name(s.name)
                        .active(true)
                        .role(s.role)
                        .passwordHash(passwordEncoder.encode(s.password))
                        .build();
                userRepository.save(u);
                log.info("Created user: {} / {} (password: {})", s.identifier, s.role, s.password);
            }
        }
    }

    private void ensureSampleRequests() {
        if (requestRepository.count() > 0) {
            log.info("Sample requests already exist; skipping.");
            return;
        }
        State registrada = stateRepository.findByCode("REGISTRADA").orElseThrow();
        State clasificada = stateRepository.findByCode("CLASIFICADA").orElseThrow();
        State enAtencion = stateRepository.findByCode("EN_ATENCION").orElseThrow();
        State atendida = stateRepository.findByCode("ATENDIDA").orElseThrow();
        State cerrada = stateRepository.findByCode("CERRADA").orElseThrow();
        Channel channel = channelRepository.findByCode("CSU").orElseThrow();
        RequestType typeConsulta = requestTypeRepository.findByCode("CONSULTA").orElseThrow();
        RequestType typeHomolog = requestTypeRepository.findByCode("HOMOLOG").orElseThrow();
        RequestType typeCupos = requestTypeRepository.findByCode("CUPOS").orElseThrow();
        User student1 = userRepository.findByIdentifier("student1").orElseThrow();
        User student2 = userRepository.findByIdentifier("student2").orElseThrow();
        User student3 = userRepository.findByIdentifier("student3").orElseThrow();
        User student4 = userRepository.findByIdentifier("student4").orElseThrow();
        User staff = userRepository.findByIdentifier("staff").orElseThrow();
        User admin = userRepository.findByIdentifier("admin").orElseThrow();

        Instant baseTime = Instant.now().minusSeconds(86400 * 5); // 5 days ago

        // Request 1: REGISTRADA only (student1)
        Request r1 = requestRepository.save(Request.builder()
                .description("Consulta sobre horarios de asignaturas del próximo semestre.")
                .registeredAt(baseTime)
                .requestType(typeConsulta)
                .channel(channel)
                .state(registrada)
                .requestedBy(student1)
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r1)
                .occurredAt(baseTime)
                .action("REGISTERED")
                .user(student1)
                .observations("Request registered")
                .build());

        // Request 2: CLASIFICADA (student2)
        Request r2 = requestRepository.save(Request.builder()
                .description("Solicitud de homologación de Matemáticas I por curso externo.")
                .registeredAt(baseTime.plusSeconds(3600))
                .requestType(typeHomolog)
                .channel(channel)
                .state(clasificada)
                .priority(Priority.HIGH)
                .priorityJustification("Homologación con fecha límite de matrícula.")
                .requestedBy(student2)
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r2)
                .occurredAt(baseTime.plusSeconds(3600))
                .action("REGISTERED")
                .user(student2)
                .observations("Request registered")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r2)
                .occurredAt(baseTime.plusSeconds(7200))
                .action("CLASSIFIED")
                .user(staff)
                .observations("Type: HOMOLOG, Priority: HIGH. Homologación con fecha límite de matrícula.")
                .build());

        // Request 3: EN_ATENCION (student3)
        Request r3 = requestRepository.save(Request.builder()
                .description("Solicitud de cupo en Cálculo II, grupo 01.")
                .registeredAt(baseTime.plusSeconds(7200))
                .requestType(typeCupos)
                .channel(channel)
                .state(enAtencion)
                .priority(Priority.HIGH)
                .priorityJustification("Cupos limitados.")
                .requestedBy(student3)
                .assignedTo(staff)
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r3)
                .occurredAt(baseTime.plusSeconds(7200))
                .action("REGISTERED")
                .user(student3)
                .observations("Request registered")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r3)
                .occurredAt(baseTime.plusSeconds(10800))
                .action("CLASSIFIED")
                .user(staff)
                .observations("Type: CUPOS, Priority: HIGH.")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r3)
                .occurredAt(baseTime.plusSeconds(14400))
                .action("ASSIGNED")
                .user(staff)
                .observations("Assigned to Staff User (staff)")
                .build());

        // Request 4: ATENDIDA (student4)
        Request r4 = requestRepository.save(Request.builder()
                .description("Consulta sobre trámite de certificados.")
                .registeredAt(baseTime.plusSeconds(10800))
                .requestType(typeConsulta)
                .channel(channel)
                .state(atendida)
                .priority(Priority.LOW)
                .priorityJustification("Consulta informativa.")
                .requestedBy(student4)
                .assignedTo(staff)
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r4)
                .occurredAt(baseTime.plusSeconds(10800))
                .action("REGISTERED")
                .user(student4)
                .observations("Request registered")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r4)
                .occurredAt(baseTime.plusSeconds(14400))
                .action("CLASSIFIED")
                .user(staff)
                .observations("Type: CONSULTA, Priority: LOW.")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r4)
                .occurredAt(baseTime.plusSeconds(18000))
                .action("ASSIGNED")
                .user(staff)
                .observations("Assigned to Staff User (staff)")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r4)
                .occurredAt(baseTime.plusSeconds(21600))
                .action("ATTENDED")
                .user(staff)
                .observations("Información enviada por correo.")
                .build());

        // Request 5: CERRADA (student1 again – so student1 has 2 requests)
        Request r5 = requestRepository.save(Request.builder()
                .description("Homologación de Física I aprobada en convenio.")
                .registeredAt(baseTime.plusSeconds(14400))
                .requestType(typeHomolog)
                .channel(channel)
                .state(cerrada)
                .priority(Priority.HIGH)
                .priorityJustification("Convenio vigente.")
                .requestedBy(student1)
                .assignedTo(staff)
                .closureObservation("Homologación aprobada. Acta registrada.")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r5)
                .occurredAt(baseTime.plusSeconds(14400))
                .action("REGISTERED")
                .user(student1)
                .observations("Request registered")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r5)
                .occurredAt(baseTime.plusSeconds(18000))
                .action("CLASSIFIED")
                .user(staff)
                .observations("Type: HOMOLOG, Priority: HIGH.")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r5)
                .occurredAt(baseTime.plusSeconds(21600))
                .action("ASSIGNED")
                .user(staff)
                .observations("Assigned to Staff User (staff)")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r5)
                .occurredAt(baseTime.plusSeconds(25200))
                .action("ATTENDED")
                .user(staff)
                .observations("Revisión de documentos.")
                .build());
        historyEntryRepository.save(HistoryEntry.builder()
                .request(r5)
                .occurredAt(baseTime.plusSeconds(28800))
                .action("CLOSED")
                .user(admin)
                .observations("Homologación aprobada. Acta registrada.")
                .build());

        log.info("Created 5 sample requests with history entries.");
    }

    private record UserSeed(String identifier, String password, String name, String role) {}
}
