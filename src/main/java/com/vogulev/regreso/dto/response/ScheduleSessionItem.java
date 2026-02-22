package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSessionItem {

    private UUID id;
    private UUID clientId;
    private String clientFullName;
    private boolean clientHasContraindications;
    private int sessionNumber;
    private OffsetDateTime scheduledAt;
    private int durationMin;
    private String status;
    private String preSessionRequest;
    private Boolean isPaid;
    private boolean telegramReminderSent;
}
