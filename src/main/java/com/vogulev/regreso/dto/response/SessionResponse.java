package com.vogulev.regreso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private UUID id;
    private int sessionNumber;
    private String status;

    // Клиент (краткая информация)
    private UUID clientId;
    private String clientFullName;
    private boolean clientHasContraindications;

    private OffsetDateTime scheduledAt;
    private int durationMin;

    // [1] Перед
    private String preSessionRequest;
    private String preSessionState;
    private Short preSessionScore;

    // [2] Транс
    private String inductionMethod;
    private String tranceDepth;
    private String inductionNotes;

    // [3] Регрессия
    private String regressionTarget;
    private String regressionPeriod;
    private String regressionSetting;

    // [4] Ключевые сцены
    private String keyScenes;
    private List<String> keyEmotions;
    private String keyInsights;
    private String symbolicImages;

    // [5] Проработка
    private String blocksReleased;
    private Boolean healingOccurred;
    private String healingNotes;

    // [6] Выход
    private String postSessionState;
    private Short postSessionScore;
    private String integrationNotes;

    // [7] Итог практика (ТОЛЬКО для практика)
    private String practitionerNotes;
    private String nextSessionPlan;

    // Финансы
    private BigDecimal price;
    private Boolean isPaid;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
