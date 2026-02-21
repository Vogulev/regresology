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
public class ClientResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private String telegramUsername;
    private boolean telegramConnected;

    private String initialRequest;
    private List<String> presentingIssues;

    private boolean hasContraindications;
    private String contraindicationsNotes;
    private boolean intakeFormCompleted;

    private String overallProgress;
    private String generalNotes;
    private Boolean isArchived;

    // Computed stats
    private int totalSessions;
    private int completedSessions;
    private OffsetDateTime lastSessionAt;
    private int activeHomeworkCount;

    private OffsetDateTime createdAt;
}
