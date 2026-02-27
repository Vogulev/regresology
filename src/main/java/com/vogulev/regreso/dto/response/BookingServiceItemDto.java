package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingServiceItemDto {
    private String name;
    private Double price;
    private Integer durationMin;
}
