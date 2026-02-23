package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LinkSessionToThemeRequest {

    @NotNull
    private UUID sessionId;

    private String notes;
}
