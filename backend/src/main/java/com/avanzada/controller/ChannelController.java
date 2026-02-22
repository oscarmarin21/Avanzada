package com.avanzada.controller;

import com.avanzada.dto.ChannelDto;
import com.avanzada.dto.RequestMapper;
import com.avanzada.repository.ChannelRepository;
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
public class ChannelController {

    private final ChannelRepository channelRepository;
    private final RequestMapper mapper;

    @GetMapping("/channels")
    public ResponseEntity<List<ChannelDto>> listChannels() {
        List<ChannelDto> body = channelRepository.findAll().stream()
                .map(mapper::toChannelDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
