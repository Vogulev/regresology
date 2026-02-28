package com.vogulev.regreso.dto.request;

import lombok.Data;

@Data
public class PublicBookingRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String selectedSlot;   // ISO-8601
    private String serviceName;
    private String clientRequest;
}
