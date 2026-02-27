package com.vogulev.regreso.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vogulev.regreso.dto.response.BookingServiceItemDto;
import lombok.Data;
import java.util.List;

@Data
public class BookingSettingsRequest {
    @JsonProperty("isEnabled")
    private Boolean isEnabled;
    private String slug;
    private Integer defaultDurationMin;
    private Integer bufferMin;
    private Integer advanceDays;
    private Boolean requireIntakeForm;
    private List<BookingServiceItemDto> services;
    private String welcomeMessage;
}
