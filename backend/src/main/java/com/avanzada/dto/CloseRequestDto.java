package com.avanzada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseRequestDto {

    @NotNull(message = "closureObservation is required")
    @NotBlank(message = "closureObservation must not be blank")
    private String closureObservation;
}
