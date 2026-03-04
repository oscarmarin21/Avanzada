package com.avanzada.controller;

import com.avanzada.dto.ChannelDto;
import com.avanzada.dto.RequestMapper;
import com.avanzada.repository.ChannelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Channels", description = "Reference data: channels through which requests are received.")
public class ChannelController {

    private final ChannelRepository channelRepository;
    private final RequestMapper mapper;

    @GetMapping("/channels")
    @Operation(
            summary = "List channels",
            description = "Returns all channels (e.g. WEB, EMAIL) that can be used when registering requests."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Channels found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChannelDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content)
    })
    public ResponseEntity<List<ChannelDto>> listChannels() {
        List<ChannelDto> body = channelRepository.findAll().stream()
                .map(mapper::toChannelDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
