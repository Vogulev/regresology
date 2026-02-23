package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateThemeRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    private OffsetDateTime firstSeenAt;
}
