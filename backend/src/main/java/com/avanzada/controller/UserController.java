package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.UserDto;
import com.avanzada.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RequestMapper mapper;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> body = userRepository.findAll().stream()
                .map(mapper::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
