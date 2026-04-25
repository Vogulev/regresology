package com.vogulev.regreso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSectionDto {

    private UUID id;
    private String code;
    private String title;
    private String content;
    private Boolean isDefault;
    private Integer position;
}
