package com.vogulev.regreso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Practitioner practitioner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(length = 255)
    private String originalFilename;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
