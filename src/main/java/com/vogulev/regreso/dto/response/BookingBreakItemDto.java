package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingBreakItemDto {
    private String startTime;
    private String endTime;
}
