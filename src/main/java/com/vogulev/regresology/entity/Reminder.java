package com.vogulev.regresology.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipientType recipientType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(nullable = false)
    private OffsetDateTime sendAt;

    private OffsetDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    private String errorMessage;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum RecipientType { CLIENT, PRACTITIONER }
    public enum Channel { TELEGRAM, EMAIL }
    public enum Status { PENDING, SENT, FAILED }
}
