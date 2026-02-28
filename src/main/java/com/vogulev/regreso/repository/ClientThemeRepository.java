package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.ClientTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с темами (запросами) клиентов.
 * Тема представляет собой терапевтический запрос или проблему, над которой ведётся работа в сессиях.
 */
public interface ClientThemeRepository extends JpaRepository<ClientTheme, UUID> {

    /**
     * Возвращает все активные (не проработанные) темы клиента,
     * отсортированные по дате создания (новые первыми).
     *
     * @param clientId идентификатор клиента
     * @return список активных тем клиента
     */
    @Query("SELECT t FROM ClientTheme t WHERE t.client.id = :clientId AND t.isResolved = false ORDER BY t.createdAt DESC")
    List<ClientTheme> findActiveByClientId(@Param("clientId") UUID clientId);

    /**
     * Возвращает количество сессий, в которых прорабатывалась указанная тема.
     *
     * @param themeId идентификатор темы
     * @return количество связанных сессий
     */
    @Query("SELECT COUNT(ts) FROM ThemeSession ts WHERE ts.id.themeId = :themeId")
    int countSessionsByThemeId(@Param("themeId") UUID themeId);

    /**
     * Возвращает все темы клиента с проверкой принадлежности практику,
     * отсортированные по дате создания (новые первыми).
     *
     * @param clientId       идентификатор клиента
     * @param practitionerId идентификатор практика
     * @return список всех тем клиента данного практика
     */
    List<ClientTheme> findByClientIdAndPractitionerIdOrderByCreatedAtDesc(UUID clientId, UUID practitionerId);

    /**
     * Возвращает тему по её идентификатору с проверкой принадлежности практику.
     *
     * @param id             идентификатор темы
     * @param practitionerId идентификатор практика
     * @return тема, если найдена и принадлежит данному практику
     */
    Optional<ClientTheme> findByIdAndPractitionerId(UUID id, UUID practitionerId);
}
