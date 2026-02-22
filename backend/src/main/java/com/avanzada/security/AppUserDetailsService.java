package com.avanzada.security;

import com.avanzada.entity.User;
import com.avanzada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByIdentifier(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("User cannot login: no password set");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UsernameNotFoundException("User is inactive");
        }
        String role = user.getRole() != null && !user.getRole().isBlank() ? user.getRole() : "STUDENT";
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role));
        return new AppUserDetails(user.getId(), user.getIdentifier(), user.getPasswordHash(), authorities);
    }
}
