package com.vogulev.regreso.dto.request;

import lombok.Data;

@Data
public class ProfileSettingsRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String bio;
    private String timezone;
    private Integer defaultSessionDurationMin;
}
