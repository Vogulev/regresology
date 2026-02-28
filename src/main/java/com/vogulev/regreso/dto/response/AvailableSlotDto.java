package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDto {
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
}
