package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.PractitionerMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<PractitionerMaterial, UUID> {

    List<PractitionerMaterial> findByPractitionerIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID practitionerId);

    List<PractitionerMaterial> findByPractitionerIdOrderByCreatedAtDesc(UUID practitionerId);

    Optional<PractitionerMaterial> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
