package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.CreateHomeworkRequest;
import com.vogulev.regreso.dto.response.HomeworkResponse;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления домашними заданиями клиентов.
 * Поддерживает создание заданий как на уровне клиента,
 * так и в рамках конкретной сессии, а также отслеживание их статуса.
 */
public interface HomeworkService {

    /**
     * Возвращает все домашние задания клиента.
     *
     * @param clientId       идентификатор клиента
     * @param practitionerId идентификатор практика
     * @return список домашних заданий клиента
     */
    List<HomeworkResponse> getClientHomework(UUID clientId, UUID practitionerId);

    /**
     * Создаёт домашнее задание, привязанное к клиенту.
     *
     * @param clientId       идентификатор клиента
     * @param request        данные задания
     * @param practitionerId идентификатор практика
     * @return созданное домашнее задание
     */
    HomeworkResponse createForClient(UUID clientId, CreateHomeworkRequest request, UUID practitionerId);

    /**
     * Создаёт домашнее задание, привязанное к конкретной сессии.
     *
     * @param sessionId      идентификатор сессии
     * @param request        данные задания
     * @param practitionerId идентификатор практика
     * @return созданное домашнее задание
     */
    HomeworkResponse createForSession(UUID sessionId, CreateHomeworkRequest request, UUID practitionerId);

    /**
     * Обновляет статус выполнения домашнего задания.
     *
     * @param id             идентификатор задания
     * @param status         новый статус задания
     * @param practitionerId идентификатор практика
     * @return обновлённое домашнее задание
     */
    HomeworkResponse updateStatus(UUID id, String status, UUID practitionerId);
}
