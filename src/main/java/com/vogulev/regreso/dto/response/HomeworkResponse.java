package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkResponse {

    private UUID id;
    private UUID sessionId;
    private UUID clientId;
    private String clientFullName;
    private UUID materialId;
    private String title;
    private String description;
    private String homeworkType;
    private LocalDate dueDate;
    private String status;
    private String clientResponse;
    private OffsetDateTime respondedAt;
    private OffsetDateTime createdAt;
}
