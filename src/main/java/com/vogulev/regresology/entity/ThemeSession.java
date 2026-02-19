package com.vogulev.regresology.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "theme_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeSession {

    @EmbeddedId
    private ThemeSessionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("themeId")
    @JoinColumn(name = "theme_id")
    private ClientTheme theme;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
