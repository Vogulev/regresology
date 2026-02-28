package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.ThemeSession;
import com.vogulev.regreso.entity.ThemeSessionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы со связями между темами клиентов и сессиями.
 * Сущность {@link ThemeSession} реализует связь многие-ко-многим между темой и сессией.
 */
public interface ThemeSessionRepository extends JpaRepository<ThemeSession, ThemeSessionId> {

    /**
     * Возвращает все записи о проработке темы в сессиях с загрузкой данных сессий (JOIN FETCH),
     * отсортированные по дате проведения сессии (новые первыми).
     *
     * @param themeId идентификатор темы
     * @return список связей тема-сессия с загруженными сущностями сессий
     */
    @Query("SELECT ts FROM ThemeSession ts JOIN FETCH ts.session s WHERE ts.id.themeId = :themeId ORDER BY s.scheduledAt DESC")
    List<ThemeSession> findByThemeIdWithSession(@Param("themeId") UUID themeId);

    /**
     * Проверяет, существует ли связь между темой и сессией по составному ключу.
     *
     * @param id составной идентификатор, включающий themeId и sessionId
     * @return {@code true}, если связь существует
     */
    boolean existsById(ThemeSessionId id);
}
