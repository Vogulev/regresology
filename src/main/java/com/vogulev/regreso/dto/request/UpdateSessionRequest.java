package com.vogulev.regreso.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class UpdateSessionRequest {

    private OffsetDateTime scheduledAt;
    private Integer durationMin;

    // [1] Перед сессией
    private String preSessionRequest;
    private String preSessionState;
    private Short preSessionScore;

    // [2] Введение в транс
    private String inductionMethod;
    private String tranceDepth;     // LIGHT | MEDIUM | DEEP
    private String inductionNotes;

    // [3] Куда ушёл клиент
    private String regressionTarget; // PAST_LIFE | CHILDHOOD | PRENATAL | BETWEEN_LIVES | OTHER
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

    // [7] Итог практика
    private String practitionerNotes;
    private String nextSessionPlan;

    // Финансы
    private BigDecimal price;
    private Boolean isPaid;
}
