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
public class SessionPrepResponse {

    private UUID sessionId;
    private OffsetDateTime scheduledAt;
    private int sessionNumber;

    // Клиент
    private UUID clientId;
    private String clientFullName;
    private LocalDate clientBirthDate;
    private boolean hasContraindications;
    private String contraindicationsNotes;
    private String initialRequest;
    private List<String> presentingIssues;

    // Последние 3 сессии
    private List<SessionSummaryResponse> recentSessions;

    // Активные темы
    private List<ClientThemeShortResponse> activeThemes;

    // Задание из прошлой сессии
    private HomeworkShortResponse lastHomework;

    // План который практик ставил на эту сессию
    private String nextSessionPlan;
}
