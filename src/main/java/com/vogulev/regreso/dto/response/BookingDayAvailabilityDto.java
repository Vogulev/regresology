package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDayAvailabilityDto {
    private Integer dayOfWeek;
    private Boolean isWorkingDay;
    private String startTime;
    private String endTime;
    private Integer slotIntervalMin;
    private List<BookingBreakItemDto> breaks;
}
