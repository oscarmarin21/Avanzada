package com.avanzada.controller;

import com.avanzada.dto.RequestMapper;
import com.avanzada.dto.UserDto;
import com.avanzada.repository.UserRepository;
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
@Tag(name = "Users", description = "Reference data: users that can be requesters or assignees.")
public class UserController {

    private final UserRepository userRepository;
    private final RequestMapper mapper;

    @GetMapping("/users")
    @Operation(
            summary = "List users",
            description = "Returns all users. Used mainly to select assignees or requesters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content)
    })
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> body = userRepository.findAll().stream()
                .map(mapper::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
