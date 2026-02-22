package com.avanzada.controller;

import com.avanzada.config.JwtUtil;
import com.avanzada.dto.LoginRequestDto;
import com.avanzada.dto.LoginResponseDto;
import com.avanzada.entity.User;
import com.avanzada.repository.UserRepository;
import com.avanzada.security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getIdentifier(), dto.getPassword()));
        AppUserDetails details = (AppUserDetails) auth.getPrincipal();
        User user = userRepository.findById(details.userId())
                .orElseThrow(() -> new IllegalStateException("User not found after auth"));
        String token = jwtUtil.generateToken(user);
        LoginResponseDto.AuthUserDto userDto = LoginResponseDto.AuthUserDto.builder()
                .id(user.getId())
                .identifier(user.getIdentifier())
                .name(user.getName())
                .role(user.getRole() != null ? user.getRole() : "STUDENT")
                .build();
        return ResponseEntity.ok(LoginResponseDto.builder()
                .token(token)
                .user(userDto)
                .build());
    }
}
