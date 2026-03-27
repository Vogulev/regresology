package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.SessionMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionMediaRepository extends JpaRepository<SessionMedia, UUID> {

    List<SessionMedia> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    java.util.Optional<SessionMedia> findByIdAndSessionId(UUID id, UUID sessionId);
}
