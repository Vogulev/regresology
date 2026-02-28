package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotsResponse {
    private List<AvailableSlotDto> slots;
}
