package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с учётными записями практиков.
 */
public interface PractitionerRepository extends JpaRepository<Practitioner, UUID> {

    /**
     * Ищет практика по адресу электронной почты.
     * Используется при аутентификации для загрузки учётных данных.
     *
     * @param email адрес электронной почты
     * @return практик с указанным email, если найден
     */
    Optional<Practitioner> findByEmail(String email);

    /**
     * Проверяет, зарегистрирован ли практик с указанным адресом электронной почты.
     * Используется при регистрации для предотвращения дублирования аккаунтов.
     *
     * @param email адрес электронной почты
     * @return {@code true}, если практик с таким email уже существует
     */
    boolean existsByEmail(String email);
}
