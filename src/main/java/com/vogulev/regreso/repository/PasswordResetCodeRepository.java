package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.PasswordResetCode;
import com.vogulev.regreso.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    Optional<PasswordResetCode> findByPractitioner(Practitioner practitioner);

    void deleteByPractitioner(Practitioner practitioner);
}
