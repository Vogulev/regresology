package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.PublicBookingRequest;
import com.vogulev.regreso.dto.response.AvailableSlotsResponse;
import com.vogulev.regreso.dto.response.PublicBookingConfirmation;
import com.vogulev.regreso.dto.response.PublicBookingPageResponse;

/**
 * Сервис публичной онлайн-записи к практику.
 * Обеспечивает работу страницы записи для клиентов без авторизации:
 * просмотр информации о практике, доступных слотах и создание брони.
 */
public interface PublicBookingService {

    /**
     * Возвращает публичную страницу записи по slug практика.
     *
     * @param slug уникальный идентификатор страницы записи практика
     * @return данные для отображения страницы записи
     */
    PublicBookingPageResponse getBookingPage(String slug);

    /**
     * Возвращает доступные для записи временные слоты на указанную дату.
     *
     * @param slug уникальный идентификатор страницы записи практика
     * @param date дата в формате ISO 8601 (YYYY-MM-DD)
     * @return список доступных временных слотов
     */
    AvailableSlotsResponse getAvailableSlots(String slug, String date);

    /**
     * Создаёт запись клиента на сессию.
     *
     * @param slug    уникальный идентификатор страницы записи практика
     * @param request данные клиента и выбранный временной слот
     * @return подтверждение успешной записи
     */
    PublicBookingConfirmation createBooking(String slug, PublicBookingRequest request);
}
