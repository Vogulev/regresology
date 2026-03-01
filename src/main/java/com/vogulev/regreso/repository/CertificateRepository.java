package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с дипломами и сертификатами практика.
 */
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    /**
     * Возвращает все сертификаты практика, отсортированные от новых к старым.
     *
     * @param practitionerId идентификатор практика
     * @return список сертификатов
     */
    List<Certificate> findByPractitionerIdOrderByCreatedAtDesc(UUID practitionerId);

    /**
     * Ищет сертификат по идентификатору и владельцу (для проверки доступа).
     *
     * @param id             идентификатор сертификата
     * @param practitionerId идентификатор практика
     * @return сертификат, если найден и принадлежит данному практику
     */
    Optional<Certificate> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
