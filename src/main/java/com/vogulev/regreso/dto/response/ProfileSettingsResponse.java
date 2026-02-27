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
public class ProfileSettingsResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String bio;
    private String timezone;
    private Integer defaultSessionDurationMin;
    private Long telegramChatId;
    private boolean telegramConnected;
    private String plan;
    private OffsetDateTime planExpiresAt;
}
