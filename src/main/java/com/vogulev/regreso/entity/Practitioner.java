package com.vogulev.regreso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "practitioners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Practitioner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    private String lastName;
    private String phone;

    @OneToOne(mappedBy = "practitioner", cascade = CascadeType.ALL, orphanRemoval = true)
    private PasswordResetCode passwordResetCode;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    private String timezone = "Europe/Moscow";
    private Long telegramChatId;
    @Builder.Default
    private Integer defaultSessionDurationMin = 120;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_template_json", columnDefinition = "jsonb")
    private String sessionTemplateJson;

    @Builder.Default
    private Integer inactiveClientReminderDays = 0;

    @Builder.Default
    private Boolean sessionRemindersEnabled = false;

    @Builder.Default
    private Boolean practitionerSessionRemindersEnabled = false;

    @Column(length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Plan plan = Plan.FREE;

    private OffsetDateTime planExpiresAt;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public enum Plan { FREE, BASIC, PRO }
}
