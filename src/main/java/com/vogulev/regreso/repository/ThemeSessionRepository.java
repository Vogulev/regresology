package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.ThemeSession;
import com.vogulev.regreso.entity.ThemeSessionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ThemeSessionRepository extends JpaRepository<ThemeSession, ThemeSessionId> {

    @Query("SELECT ts FROM ThemeSession ts JOIN FETCH ts.session s WHERE ts.id.themeId = :themeId ORDER BY s.scheduledAt DESC")
    List<ThemeSession> findByThemeIdWithSession(@Param("themeId") UUID themeId);

    boolean existsById(ThemeSessionId id);
}
