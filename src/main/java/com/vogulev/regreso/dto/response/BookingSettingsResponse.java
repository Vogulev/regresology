package com.vogulev.regreso.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSettingsResponse {
    @JsonProperty("isEnabled")
    private boolean isEnabled;
    private String slug;
    private String bookingUrl;
    private Integer defaultDurationMin;
    private Integer bufferMin;
    private Integer advanceDays;
    private boolean requireIntakeForm;
    private List<BookingServiceItemDto> services;
    private String welcomeMessage;
}
