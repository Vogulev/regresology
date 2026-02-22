package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByPractitionerIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID practitionerId);

    Optional<Client> findByIdAndPractitionerId(UUID id, UUID practitionerId);

    Optional<Client> findByTelegramChatId(Long telegramChatId);

    /**
     * Клиенты практика без сессий после cutoff (для уведомлений о реактивации).
     * Включает клиентов с нулём сессий.
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
