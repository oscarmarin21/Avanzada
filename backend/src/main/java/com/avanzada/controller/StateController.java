package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.StateDto;
import com.avanzada.repository.StateRepository;
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
public class StateController {

    private final StateRepository stateRepository;
    private final RequestMapper mapper;

    @GetMapping("/states")
    public ResponseEntity<List<StateDto>> listStates() {
        List<StateDto> body = stateRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(mapper::toStateDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
