package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.BookingSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BookingSettingsRepository extends JpaRepository<BookingSettings, UUID> {
    Optional<BookingSettings> findByPractitionerId(UUID practitionerId);
}
