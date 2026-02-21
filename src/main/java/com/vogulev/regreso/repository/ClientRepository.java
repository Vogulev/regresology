package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByPractitionerIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID practitionerId);

    Optional<Client> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
