package com.avanzada.controller;

import com.avanzada.config.JwtUtil;
import com.avanzada.dto.LoginRequestDto;
import com.avanzada.dto.LoginResponseDto;
import com.avanzada.entity.User;
import com.avanzada.repository.UserRepository;
import com.avanzada.security.AppUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and JWT token management.")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user and return JWT token",
            description = "Authenticates a user using identifier and password, returning a JWT token to be used in subsequent requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated",
                    content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content)
    })
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
