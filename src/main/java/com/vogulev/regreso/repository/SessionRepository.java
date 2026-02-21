package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByClientIdOrderByScheduledAtDesc(UUID clientId);

    long countByClientId(UUID clientId);

    long countByClientIdAndStatus(UUID clientId, Session.Status status);

    Optional<Session> findFirstByClientIdOrderByScheduledAtDesc(UUID clientId);

    Optional<Session> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
