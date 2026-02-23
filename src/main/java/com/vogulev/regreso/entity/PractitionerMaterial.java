package com.vogulev.regreso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "practitioner_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PractitionerMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Practitioner practitioner;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String materialType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private Boolean isArchived = false;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
