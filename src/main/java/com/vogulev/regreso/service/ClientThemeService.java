package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.CreateThemeRequest;
import com.vogulev.regreso.dto.request.LinkSessionToThemeRequest;
import com.vogulev.regreso.dto.response.ClientThemeResponse;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления терапевтическими темами клиента.
 * Позволяет создавать темы, связывать их с сессиями
 * и отмечать как проработанные.
 */
public interface ClientThemeService {

    /**
     * Возвращает список всех тем клиента.
     *
     * @param clientId       идентификатор клиента
     * @param practitionerId идентификатор практика
     * @return список тем клиента
     */
    List<ClientThemeResponse> getClientThemes(UUID clientId, UUID practitionerId);

    /**
     * Создаёт новую тему для клиента.
     *
     * @param clientId       идентификатор клиента
     * @param request        данные новой темы
     * @param practitionerId идентификатор практика
     * @return созданная тема
     */
    ClientThemeResponse createTheme(UUID clientId, CreateThemeRequest request, UUID practitionerId);

    /**
     * Связывает тему с конкретной сессией.
     *
     * @param themeId        идентификатор темы
     * @param request        данные о сессии для привязки
     * @param practitionerId идентификатор практика
     * @return обновлённая тема с привязанной сессией
     */
    ClientThemeResponse linkSession(UUID themeId, LinkSessionToThemeRequest request, UUID practitionerId);

    /**
     * Помечает тему как проработанную (завершённую).
     *
     * @param themeId        идентификатор темы
     * @param practitionerId идентификатор практика
     * @return обновлённая тема со статусом «проработана»
     */
    ClientThemeResponse resolveTheme(UUID themeId, UUID practitionerId);
}
