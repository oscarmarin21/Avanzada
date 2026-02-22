package com.avanzada.config;

import com.avanzada.entity.User;
import com.avanzada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures at least one user can log in when no user has a password set (e.g. fresh or legacy DB).
 * Sets first user's password to "admin123" and role to ADMIN if unset. Does nothing if any user already has a password.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DevAuthBootstrap {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureOneUserCanLogin() {
        List<User> all = userRepository.findAll();
        boolean anyWithPassword = all.stream()
                .anyMatch(u -> u.getPasswordHash() != null && !u.getPasswordHash().isBlank());
        if (anyWithPassword) return;
        User first = all.stream().findFirst().orElse(null);
        if (first == null) return;
        String hash = passwordEncoder.encode("admin123");
        first.setPasswordHash(hash);
        if (first.getRole() == null || first.getRole().isBlank()) {
            first.setRole("ADMIN");
        }
        userRepository.save(first);
        log.info("Dev auth bootstrap: set password for user {} (identifier={}). Use password: admin123", first.getId(), first.getIdentifier());
    }
}
