package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CreateSessionRequest {

    @NotNull
    private UUID clientId;

    @NotNull
    private OffsetDateTime scheduledAt;

    private Integer durationMin;

    @Size(max = 255)
    private String title;

    private String preSessionRequest;

    private BigDecimal price;
}
