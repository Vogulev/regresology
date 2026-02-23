package com.vogulev.regreso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private OffsetDateTime scheduledAt;

    @Builder.Default
    private Integer durationMin = 120;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.SCHEDULED;

    private Integer sessionNumber;

    // [1] Перед сессией
    @Column(columnDefinition = "TEXT") private String preSessionRequest;
    @Column(columnDefinition = "TEXT") private String preSessionState;
    private Short preSessionScore;

    // [2] Введение в транс
    private String inductionMethod;

    @Enumerated(EnumType.STRING)
    private TranceDepth tranceDepth;

    @Column(columnDefinition = "TEXT") private String inductionNotes;

    // [3] Куда ушёл клиент
    @Enumerated(EnumType.STRING)
    private RegressionTarget regressionTarget;

    private String regressionPeriod;

    @Column(columnDefinition = "TEXT") private String regressionSetting;

    // [4] Ключевые сцены
    @Column(columnDefinition = "TEXT") private String keyScenes;

    @Column(columnDefinition = "TEXT[]")
    private String[] keyEmotions;

    @Column(columnDefinition = "TEXT") private String keyInsights;
    @Column(columnDefinition = "TEXT") private String symbolicImages;

    // [5] Проработка
    @Column(columnDefinition = "TEXT") private String blocksReleased;

    @Builder.Default
    private Boolean healingOccurred = false;

    @Column(columnDefinition = "TEXT") private String healingNotes;

    // [6] Выход
    @Column(columnDefinition = "TEXT") private String postSessionState;
    private Short postSessionScore;
    @Column(columnDefinition = "TEXT") private String integrationNotes;

    // [7] Итог практика (приватные заметки — клиенту не показывать!)
    @Column(columnDefinition = "TEXT") private String practitionerNotes;
    @Column(columnDefinition = "TEXT") private String nextSessionPlan;

    // AI саммари
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    private OffsetDateTime aiSummaryGeneratedAt;

    // Финансы + флаги
    private BigDecimal price;

    @Builder.Default
    private Boolean isPaid = false;

    @Builder.Default
    private Boolean reminder24hSent = false;

    @Builder.Default
    private Boolean reminder1hSent = false;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public enum Status { SCHEDULED, COMPLETED, CANCELLED, NO_SHOW }
    public enum TranceDepth { LIGHT, MEDIUM, DEEP }
    public enum RegressionTarget { PAST_LIFE, CHILDHOOD, PRENATAL, BETWEEN_LIVES, OTHER }
}
