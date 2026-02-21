package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientListItemResponse {

    private UUID id;
    private String fullName;
    private String phone;
    private LocalDate birthDate;
    private boolean hasContraindications;
    private boolean telegramConnected;

    // Computed stats
    private int totalSessions;
    private OffsetDateTime lastSessionAt;
    private List<String> presentingIssues;
    private int activeHomeworkCount;

    private Boolean isArchived;
}
