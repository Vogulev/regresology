package com.vogulev.regreso.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Событие завершения сессии — публикуется после смены статуса на COMPLETED.
 * В будущем подписывается AiSummaryService для асинхронной генерации саммари.
 */
@Getter
public class SessionCompletedEvent extends ApplicationEvent {

    private final UUID sessionId;

    public SessionCompletedEvent(Object source, UUID sessionId) {
        super(source);
        this.sessionId = sessionId;
    }
}
