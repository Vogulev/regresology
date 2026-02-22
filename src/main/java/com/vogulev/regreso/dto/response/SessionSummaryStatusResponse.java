package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ответ для polling AI-саммари сессии.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryStatusResponse {

    private UUID sessionId;
    private String status; // PENDING | READY | FAILED
    private String summary;
    private OffsetDateTime generatedAt;
}
