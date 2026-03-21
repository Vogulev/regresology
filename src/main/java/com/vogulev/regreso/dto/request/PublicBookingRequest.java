package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PublicBookingRequest {
    private String firstName;
    private String lastName;
    private String phone;
    @Pattern(
            regexp = "^$|^@?[A-Za-z][A-Za-z0-9_]{4,31}$",
            message = "Некорректный Telegram username. Пример: @TelegramUsername"
    )
    private String telegramUsername;
    private String selectedSlot;   // ISO-8601
    private String serviceName;
    private String clientRequest;
}
