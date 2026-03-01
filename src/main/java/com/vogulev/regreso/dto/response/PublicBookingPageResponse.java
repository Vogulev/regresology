package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicBookingPageResponse {
    private String practitionerName;
    private String practitionerBio;
    private String practitionerPhotoUrl;
    private String welcomeMessage;
    private List<BookingServiceItemDto> services;
    private List<CertificateResponse> certificates;
    private boolean requireIntakeForm;
}
