package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateHomeworkStatusRequest {

    @NotBlank
    private String status; // COMPLETED | SKIPPED
}
