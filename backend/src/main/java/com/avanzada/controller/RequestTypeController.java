package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.RequestTypeDto;
import com.avanzada.repository.RequestTypeRepository;
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
public class RequestTypeController {

    private final RequestTypeRepository requestTypeRepository;
    private final RequestMapper mapper;

    @GetMapping("/request-types")
    public ResponseEntity<List<RequestTypeDto>> listRequestTypes() {
        List<RequestTypeDto> body = requestTypeRepository.findAll().stream()
                .map(mapper::toRequestTypeDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
