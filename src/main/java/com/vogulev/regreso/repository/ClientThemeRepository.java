package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.ClientTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClientThemeRepository extends JpaRepository<ClientTheme, UUID> {

    @Query("SELECT t FROM ClientTheme t WHERE t.client.id = :clientId AND t.isResolved = false ORDER BY t.createdAt DESC")
    List<ClientTheme> findActiveByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(ts) FROM ThemeSession ts WHERE ts.id.themeId = :themeId")
    int countSessionsByThemeId(@Param("themeId") UUID themeId);
}
