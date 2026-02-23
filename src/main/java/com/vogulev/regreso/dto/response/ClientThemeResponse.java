package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientThemeResponse {

    private UUID id;
    private String title;
    private String description;
    private Boolean isResolved;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime firstSeenAt;
    private int sessionsCount;
    private List<SessionSummaryShort> sessions;
}
