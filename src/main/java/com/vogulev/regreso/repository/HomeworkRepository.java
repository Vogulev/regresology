package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkRepository extends JpaRepository<Homework, UUID> {

    long countByClientIdAndStatus(UUID clientId, Homework.Status status);

    Optional<Homework> findFirstByClientIdOrderByCreatedAtDesc(UUID clientId);

    Optional<Homework> findFirstByClientIdAndStatusOrderByCreatedAtDesc(UUID clientId, Homework.Status status);

    List<Homework> findByClientIdAndPractitionerIdOrderByCreatedAtDesc(UUID clientId, UUID practitionerId);

    Optional<Homework> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
