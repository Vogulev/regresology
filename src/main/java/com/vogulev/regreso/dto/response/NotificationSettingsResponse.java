package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponse {
    private boolean sessionRemindersEnabled;
    private Integer inactiveClientReminderDays;
    private boolean telegramConnected;
}
