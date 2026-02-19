package com.vogulev.regresology.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false, length = 20)
    private String mediaType;

    @Column(nullable = false, length = 500)
    private String fileKey;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    private Integer fileSizeBytes;
    private Integer durationSec;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
