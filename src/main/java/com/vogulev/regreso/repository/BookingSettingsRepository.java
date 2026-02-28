package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.BookingSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с настройками онлайн-записи практика.
 */
public interface BookingSettingsRepository extends JpaRepository<BookingSettings, UUID> {

    /**
     * Возвращает настройки онлайн-записи для указанного практика.
     *
     * @param practitionerId идентификатор практика
     * @return настройки онлайн-записи, если существуют
     */
    Optional<BookingSettings> findByPractitionerId(UUID practitionerId);

    /**
     * Возвращает активные настройки онлайн-записи по публичному slug-у.
     * Используется на публичной странице записи {@code /book/{slug}} без авторизации.
     *
     * @param slug уникальный публичный идентификатор страницы записи
     * @return настройки онлайн-записи, если страница существует и включена
     */
    Optional<BookingSettings> findBySlugAndIsEnabledTrue(String slug);
}
