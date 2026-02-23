package com.vogulev.regreso.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateHomeworkRequest {

    private UUID materialId;

    // Optional when materialId provided — service fills from material if blank
    private String title;
    private String description;

    private String homeworkType; // JOURNALING | MEDITATION | BODYWORK | OBSERVATION | OTHER

    private LocalDate dueDate;
}
