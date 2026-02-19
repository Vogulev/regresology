package com.vogulev.regresology.repository;

import com.vogulev.regresology.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PractitionerRepository extends JpaRepository<Practitioner, UUID> {

    Optional<Practitioner> findByEmail(String email);

    boolean existsByEmail(String email);
}
