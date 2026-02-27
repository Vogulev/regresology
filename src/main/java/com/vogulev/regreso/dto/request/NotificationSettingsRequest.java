package com.vogulev.regreso.dto.request;

import lombok.Data;

@Data
public class NotificationSettingsRequest {
    private Boolean sessionRemindersEnabled;
    private Integer inactiveClientReminderDays;
}
