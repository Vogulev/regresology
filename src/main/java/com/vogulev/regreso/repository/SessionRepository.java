package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByClientIdOrderByScheduledAtDesc(UUID clientId);

    long countByClientId(UUID clientId);

    long countByClientIdAndStatus(UUID clientId, Session.Status status);

    Optional<Session> findFirstByClientIdOrderByScheduledAtDesc(UUID clientId);

    Optional<Session> findByIdAndPractitionerId(UUID id, UUID practitionerId);

    List<Session> findByPractitionerIdAndScheduledAtBetweenOrderByScheduledAt(
            UUID practitionerId, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COALESCE(MAX(s.sessionNumber), 0) + 1 FROM Session s WHERE s.client.id = :clientId")
    Integer getNextSessionNumber(@Param("clientId") UUID clientId);

    List<Session> findTop3ByClientIdOrderByScheduledAtDesc(UUID clientId);

    @Query("SELECT s FROM Session s WHERE s.client.id = :clientId AND s.status = 'COMPLETED' AND s.aiSummary IS NOT NULL ORDER BY s.scheduledAt DESC LIMIT 10")
    List<Session> findTop10CompletedWithSummaryByClientId(@Param("clientId") UUID clientId);
}
