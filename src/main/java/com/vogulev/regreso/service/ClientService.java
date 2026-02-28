package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.ClientRequest;
import com.vogulev.regreso.dto.response.ClientListItemResponse;
import com.vogulev.regreso.dto.response.ClientResponse;
import com.vogulev.regreso.dto.response.SessionListItemResponse;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления клиентами практика.
 * Предоставляет операции создания, редактирования, архивирования
 * клиентов, а также работы с их прогрессом и историей сессий.
 */
public interface ClientService {

    /**
     * Возвращает список всех клиентов практика.
     *
     * @param practitionerId идентификатор практика
     * @return список кратких данных о клиентах
     */
    List<ClientListItemResponse> getClients(UUID practitionerId);

    /**
     * Возвращает полные данные конкретного клиента.
     *
     * @param id             идентификатор клиента
     * @param practitionerId идентификатор практика
     * @return полная карточка клиента
     */
    ClientResponse getClient(UUID id, UUID practitionerId);

    /**
     * Создаёт нового клиента для практика.
     *
     * @param request        данные нового клиента
     * @param practitionerId идентификатор практика
     * @return созданная карточка клиента
     */
    ClientResponse createClient(ClientRequest request, UUID practitionerId);

    /**
     * Обновляет данные существующего клиента.
     *
     * @param id             идентификатор клиента
     * @param request        новые данные клиента
     * @param practitionerId идентификатор практика
     * @return обновлённая карточка клиента
     */
    ClientResponse updateClient(UUID id, ClientRequest request, UUID practitionerId);

    /**
     * Переводит клиента в архив или восстанавливает из архива.
     *
     * @param id             идентификатор клиента
     * @param archive        {@code true} — архивировать, {@code false} — разархивировать
     * @param practitionerId идентификатор практика
     */
    void archiveClient(UUID id, boolean archive, UUID practitionerId);

    /**
     * Обновляет общий прогресс клиента.
     *
     * @param id              идентификатор клиента
     * @param overallProgress текстовое описание прогресса
     * @param practitionerId  идентификатор практика
     */
    void updateProgress(UUID id, String overallProgress, UUID practitionerId);

    /**
     * Возвращает историю сессий клиента.
     *
     * @param clientId       идентификатор клиента
     * @param practitionerId идентификатор практика
     * @return список кратких данных о сессиях клиента
     */
    List<SessionListItemResponse> getClientSessions(UUID clientId, UUID practitionerId);

    /**
     * Запускает асинхронную генерацию AI-резюме по всем сессиям клиента.
     *
     * @param clientId       идентификатор клиента
     * @param practitionerId идентификатор практика
     */
    void triggerClientSummaryGeneration(UUID clientId, UUID practitionerId);
}
