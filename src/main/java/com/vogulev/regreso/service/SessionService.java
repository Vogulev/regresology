package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.CancelSessionRequest;
import com.vogulev.regreso.dto.request.CreateSessionRequest;
import com.vogulev.regreso.dto.request.UpdateSessionRequest;
import com.vogulev.regreso.dto.response.SessionMediaResponse;
import com.vogulev.regreso.dto.response.SessionPrepResponse;
import com.vogulev.regreso.dto.response.SessionResponse;
import com.vogulev.regreso.dto.response.SessionSummaryStatusResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Сервис для управления сессиями регрессионной терапии.
 * Обеспечивает полный жизненный цикл сессии: создание, заполнение протокола,
 * завершение, отмену, а также подготовку и генерацию AI-резюме.
 */
public interface SessionService {

    /**
     * Создаёт новую сессию для клиента.
     *
     * @param request        данные новой сессии (клиент, дата, время)
     * @param practitionerId идентификатор практика
     * @return созданная сессия
     */
    SessionResponse createSession(CreateSessionRequest request, UUID practitionerId);

    /**
     * Возвращает данные сессии по идентификатору.
     *
     * @param sessionId      идентификатор сессии
     * @param practitionerId идентификатор практика
     * @return данные сессии с протоколом
     */
    SessionResponse getSession(UUID sessionId, UUID practitionerId);

    /**
     * Обновляет данные протокола сессии (автосохранение).
     *
     * @param sessionId      идентификатор сессии
     * @param request        обновлённые данные протокола
     * @param practitionerId идентификатор практика
     * @return обновлённые данные сессии
     */
    SessionResponse updateSession(UUID sessionId, UpdateSessionRequest request, UUID practitionerId);

    /**
     * Завершает сессию и сохраняет финальные данные протокола.
     * После завершения автоматически запускается генерация AI-резюме.
     *
     * @param sessionId      идентификатор сессии
     * @param request        финальные данные протокола
     * @param practitionerId идентификатор практика
     * @return данные завершённой сессии
     */
    SessionResponse completeSession(UUID sessionId, UpdateSessionRequest request, UUID practitionerId);

    /**
     * Отменяет сессию с указанием причины.
     *
     * @param sessionId      идентификатор сессии
     * @param request        причина отмены
     * @param practitionerId идентификатор практика
     * @return данные отменённой сессии
     */
    SessionResponse cancelSession(UUID sessionId, CancelSessionRequest request, UUID practitionerId);

    /**
     * Возвращает подготовительные данные перед сессией.
     * Включает информацию о клиенте, активных темах, противопоказаниях
     * и резюме предыдущей сессии.
     *
     * @param sessionId      идентификатор сессии
     * @param practitionerId идентификатор практика
     * @return данные для подготовки к сессии
     */
    SessionPrepResponse getSessionPrep(UUID sessionId, UUID practitionerId);

    /**
     * Возвращает статус и текст AI-резюме сессии.
     *
     * @param sessionId      идентификатор сессии
     * @param practitionerId идентификатор практика
     * @return статус генерации и текст резюме (если готово)
     */
    SessionSummaryStatusResponse getSessionSummary(UUID sessionId, UUID practitionerId);

    /**
     * Запускает асинхронную генерацию AI-резюме по протоколу сессии.
     *
     * @param sessionId      идентификатор сессии
     * @param practitionerId идентификатор практика
     */
    void triggerSessionSummaryGeneration(UUID sessionId, UUID practitionerId);

    /**
     * Загружает фото рукописного протокола или других материалов к сессии.
     *
     * @param sessionId      идентификатор сессии
     * @param file           изображение
     * @param caption        подпись к фото
     * @param practitionerId идентификатор практика
     * @return данные сохранённого медиафайла
     * @throws IOException если файл не удалось сохранить
     */
    SessionMediaResponse uploadSessionPhoto(UUID sessionId, MultipartFile file, String caption, UUID practitionerId)
            throws IOException;

    /**
     * Удаляет фото протокола из сессии.
     *
     * @param sessionId      идентификатор сессии
     * @param mediaId        идентификатор фото
     * @param practitionerId идентификатор практика
     */
    void deleteSessionPhoto(UUID sessionId, UUID mediaId, UUID practitionerId);
}
