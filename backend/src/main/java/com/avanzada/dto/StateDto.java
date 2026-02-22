package com.avanzada.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateDto {

    private Long id;
    private String code;
    private String name;
    private Integer order;
}
