package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.BookingSettingsRequest;
import com.vogulev.regreso.dto.request.NotificationSettingsRequest;
import com.vogulev.regreso.dto.request.ProfileSettingsRequest;
import com.vogulev.regreso.dto.response.BookingSettingsResponse;
import com.vogulev.regreso.dto.response.NotificationSettingsResponse;
import com.vogulev.regreso.dto.response.ProfileSettingsResponse;

import java.util.UUID;

/**
 * Сервис для управления настройками практика.
 * Предоставляет доступ к профилю, параметрам онлайн-записи
 * и настройкам уведомлений.
 */
public interface SettingsService {

    /**
     * Возвращает профиль практика.
     *
     * @param practitionerId идентификатор практика
     * @return данные профиля практика
     */
    ProfileSettingsResponse getProfile(UUID practitionerId);

    /**
     * Обновляет профиль практика.
     *
     * @param practitionerId идентификатор практика
     * @param request        новые данные профиля
     * @return обновлённый профиль практика
     */
    ProfileSettingsResponse updateProfile(UUID practitionerId, ProfileSettingsRequest request);

    /**
     * Возвращает настройки онлайн-записи практика.
     *
     * @param practitionerId идентификатор практика
     * @return текущие настройки онлайн-записи
     */
    BookingSettingsResponse getBookingSettings(UUID practitionerId);

    /**
     * Обновляет настройки онлайн-записи практика.
     *
     * @param practitionerId идентификатор практика
     * @param request        новые настройки онлайн-записи
     * @return обновлённые настройки онлайн-записи
     */
    BookingSettingsResponse updateBookingSettings(UUID practitionerId, BookingSettingsRequest request);

    /**
     * Возвращает настройки уведомлений практика.
     *
     * @param practitionerId идентификатор практика
     * @return текущие настройки уведомлений
     */
    NotificationSettingsResponse getNotificationSettings(UUID practitionerId);

    /**
     * Обновляет настройки уведомлений практика.
     *
     * @param practitionerId идентификатор практика
     * @param request        новые настройки уведомлений
     * @return обновлённые настройки уведомлений
     */
    NotificationSettingsResponse updateNotificationSettings(UUID practitionerId, NotificationSettingsRequest request);
}
