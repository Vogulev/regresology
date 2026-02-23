package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialListItemResponse {

    private UUID id;
    private String title;
    private String materialType;
    private Boolean isArchived;
    private OffsetDateTime createdAt;
}
