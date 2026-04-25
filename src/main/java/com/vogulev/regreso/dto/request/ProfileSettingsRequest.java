package com.vogulev.regreso.dto.request;

import com.vogulev.regreso.dto.SessionSectionDto;
import lombok.Data;

import java.util.List;

@Data
public class ProfileSettingsRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String bio;
    private String timezone;
    private Integer defaultSessionDurationMin;
    private List<SessionSectionDto> sessionTemplateSections;
}
