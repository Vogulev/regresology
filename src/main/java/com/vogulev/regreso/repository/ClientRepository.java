package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с клиентами практика.
 */
public interface ClientRepository extends JpaRepository<Client, UUID> {

    /**
     * Возвращает всех активных (не архивированных) клиентов практика,
     * отсортированных по дате создания (новые первыми).
     *
     * @param practitionerId идентификатор практика
     * @return список активных клиентов
     */
    List<Client> findByPractitionerIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID practitionerId);

    /**
     * Возвращает клиента по его идентификатору с проверкой принадлежности практику.
     * Используется для защиты от доступа к чужим клиентам.
     *
     * @param id             идентификатор клиента
     * @param practitionerId идентификатор практика
     * @return клиент, если найден и принадлежит данному практику
     */
    Optional<Client> findByIdAndPractitionerId(UUID id, UUID practitionerId);

    /**
     * Ищет активного (не архивированного) клиента практика по номеру телефона.
     * Используется для предотвращения создания дубликатов при онлайн-записи.
     *
     * @param practitionerId идентификатор практика
     * @param phone          номер телефона клиента
     * @return клиент с указанным телефоном, если найден и не архивирован
     */
    Optional<Client> findByPractitionerIdAndPhoneAndIsArchivedFalse(UUID practitionerId, String phone);

    /**
     * Возвращает клиента по идентификатору Telegram-чата.
     * Используется Telegram-ботом для идентификации клиента по входящему сообщению.
     *
     * @param telegramChatId идентификатор Telegram-чата клиента
     * @return клиент, привязанный к данному Telegram-чату, если найден
     */
    Optional<Client> findByTelegramChatId(Long telegramChatId);

    /**
     * Клиенты практика без сессий после cutoff (для уведомлений о реактивации).
     * Включает клиентов с нулём сессий.
     *
     * @param practitionerId идентификатор практика
     * @param cutoff         точка отсчёта: клиенты без сессий начиная с этой даты считаются неактивными
     * @return список неактивных клиентов
     */
    @Query("""
            SELECT c FROM Client c
            WHERE c.practitioner.id = :practitionerId
              AND c.isArchived = false
              AND NOT EXISTS (
                  SELECT s FROM Session s
                  WHERE s.client.id = c.id
                    AND s.scheduledAt >= :cutoff
              )
            """)
    List<Client> findInactiveClients(@Param("practitionerId") UUID practitionerId,
                                     @Param("cutoff") OffsetDateTime cutoff);
}
