package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с сессиями клиентов.
 */
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Возвращает все сессии клиента, отсортированные по дате проведения (новые первыми).
     *
     * @param clientId идентификатор клиента
     * @return список всех сессий клиента
     */
    List<Session> findByClientIdOrderByScheduledAtDesc(UUID clientId);

    /**
     * Возвращает общее количество сессий клиента.
     *
     * @param clientId идентификатор клиента
     * @return суммарное количество сессий
     */
    long countByClientId(UUID clientId);

    /**
     * Возвращает количество сессий клиента с указанным статусом.
     *
     * @param clientId идентификатор клиента
     * @param status   статус сессии
     * @return количество сессий с данным статусом
     */
    long countByClientIdAndStatus(UUID clientId, Session.Status status);

    /**
     * Возвращает последнюю по дате сессию клиента.
     *
     * @param clientId идентификатор клиента
     * @return последняя сессия клиента, если существует
     */
    Optional<Session> findFirstByClientIdOrderByScheduledAtDesc(UUID clientId);

    /**
     * Возвращает сессию по её идентификатору с проверкой принадлежности практику.
     * Используется для защиты от доступа к чужим сессиям.
     *
     * @param id             идентификатор сессии
     * @param practitionerId идентификатор практика
     * @return сессия, если найдена и принадлежит данному практику
     */
    Optional<Session> findByIdAndPractitionerId(UUID id, UUID practitionerId);

    /**
     * Возвращает все сессии практика в заданном временном диапазоне,
     * отсортированные по времени начала (ранние первыми).
     * Используется для формирования расписания на день или неделю.
     *
     * @param practitionerId идентификатор практика
     * @param from           начало временного диапазона (включительно)
     * @param to             конец временного диапазона (включительно)
     * @return список сессий практика в указанном диапазоне
     */
    List<Session> findByPractitionerIdAndScheduledAtBetweenOrderByScheduledAt(
            UUID practitionerId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Возвращает следующий порядковый номер сессии для данного клиента.
     * Вычисляется как максимальный существующий номер плюс один;
     * если сессий нет, возвращает 1.
     *
     * @param clientId идентификатор клиента
     * @return следующий порядковый номер сессии
     */
    @Query("SELECT COALESCE(MAX(s.sessionNumber), 0) + 1 FROM Session s WHERE s.client.id = :clientId")
    Integer getNextSessionNumber(@Param("clientId") UUID clientId);

    /**
     * Возвращает три последние сессии клиента по дате проведения.
     * Используется для краткого отображения истории на экране подготовки.
     *
     * @param clientId идентификатор клиента
     * @return список из не более чем трёх последних сессий клиента
     */
    List<Session> findTop3ByClientIdOrderByScheduledAtDesc(UUID clientId);

    /**
     * Возвращает до десяти последних завершённых сессий клиента, для которых сформировано AI-саммари.
     * Используется при генерации общего саммари динамики клиента.
     *
     * @param clientId идентификатор клиента
     * @return список завершённых сессий с AI-саммари (не более 10), отсортированных по дате (новые первыми)
     */
    @Query("SELECT s FROM Session s WHERE s.client.id = :clientId AND s.status = 'COMPLETED' AND s.aiSummary IS NOT NULL ORDER BY s.scheduledAt DESC LIMIT 10")
    List<Session> findTop10CompletedWithSummaryByClientId(@Param("clientId") UUID clientId);
}
