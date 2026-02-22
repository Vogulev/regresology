package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Краткое саммари сессии — для экрана подготовки к следующей сессии.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryResponse {

    private UUID id;
    private int sessionNumber;
    private OffsetDateTime scheduledAt;
    private String regressionTarget;
    private String regressionPeriod;
    private String keyInsights;
    private String blocksReleased;
}
