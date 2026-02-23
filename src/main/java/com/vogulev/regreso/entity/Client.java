package com.vogulev.regreso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private Practitioner practitioner;

    @Column(nullable = false)
    private String firstName;

    private String lastName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private String telegramUsername;
    private Long telegramChatId;

    @Column(columnDefinition = "TEXT")
    private String initialRequest;

    @Column(columnDefinition = "TEXT[]")
    private String[] presentingIssues;

    @Builder.Default
    private Boolean hasContraindications = false;

    @Column(columnDefinition = "TEXT")
    private String contraindicationsNotes;

    @Builder.Default
    private Boolean intakeFormCompleted = false;

    @Column(columnDefinition = "TEXT")
    private String overallProgress;

    @Column(columnDefinition = "TEXT")
    private String generalNotes;

    @Builder.Default
    private Boolean isArchived = false;

    // AI саммари
    @Column(columnDefinition = "TEXT")
    private String aiOverallSummary;

    private OffsetDateTime aiOverallSummaryGeneratedAt;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public String getFullName() {
        return lastName != null ? firstName + " " + lastName : firstName;
    }
}
