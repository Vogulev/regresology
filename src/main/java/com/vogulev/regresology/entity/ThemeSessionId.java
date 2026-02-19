package com.vogulev.regresology.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ThemeSessionId implements Serializable {

    @Column(name = "theme_id")
    private UUID themeId;

    @Column(name = "session_id")
    private UUID sessionId;
}
