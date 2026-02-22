package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.NotNull;
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

    private String preSessionRequest;

    private BigDecimal price;
}
