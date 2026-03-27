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
public class SessionMediaResponse {

    private UUID id;
    private String mediaType;
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Integer fileSizeBytes;
    private Integer durationSec;
    private String caption;
    private OffsetDateTime createdAt;
}
