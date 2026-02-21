package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProgressRequest {

    @NotBlank(message = "Прогресс не может быть пустым")
    private String overallProgress;
}
