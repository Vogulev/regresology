package com.vogulev.regreso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false, unique = true)
    private Practitioner practitioner;

    @Builder.Default
    private Boolean isEnabled = false;

    @Column(unique = true, length = 100)
    private String slug;

    @Builder.Default
    private Integer defaultDurationMin = 120;

    @Builder.Default
    private Integer bufferMin = 30;

    @Builder.Default
    private Integer advanceDays = 30;

    @Builder.Default
    private Boolean requireIntakeForm = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String services;

    @Column(columnDefinition = "TEXT")
    private String welcomeMessage;

    @Builder.Default
    @Column(length = 20)
    private String availabilityMode = "DEFAULT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String weeklyAvailability;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
