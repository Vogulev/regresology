package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionListItemResponse {

    private UUID id;
    private Integer sessionNumber;
    private String status;
    private OffsetDateTime scheduledAt;
    private Integer durationMin;
    private String preSessionRequest;
    private String regressionTarget;
    private String keyInsights;
    private BigDecimal price;
    private Boolean isPaid;
}
