package com.vogulev.regreso.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MaterialRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotBlank
    @Size(max = 50)
    private String materialType;

    private String content;
}
