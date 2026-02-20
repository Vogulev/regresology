package com.vogulev.regreso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner;

    @Column(nullable = false, length = 20)
    private String plan;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    private String paymentProvider;
    private String externalId;
    private BigDecimal amount;

    @Builder.Default
    private String currency = "RUB";

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
