package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.BookingSettingsRequest;
import com.vogulev.regreso.dto.request.NotificationSettingsRequest;
import com.vogulev.regreso.dto.request.ProfileSettingsRequest;
import com.vogulev.regreso.dto.response.BookingSettingsResponse;
import com.vogulev.regreso.dto.response.CertificateResponse;
import com.vogulev.regreso.dto.response.NotificationSettingsResponse;
import com.vogulev.regreso.dto.response.ProfileSettingsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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

    /**
     * Загружает фото профиля практика, удаляет старое и возвращает обновлённый профиль.
     *
     * @param practitionerId идентификатор практика
     * @param file           файл изображения (JPEG, PNG, WebP)
     * @return обновлённый профиль с новым photoUrl
     * @throws IOException если не удалось сохранить файл
     */
    ProfileSettingsResponse uploadPhoto(UUID practitionerId, MultipartFile file) throws IOException;

    /**
     * Возвращает список всех сертификатов практика.
     *
     * @param practitionerId идентификатор практика
     * @return список сертификатов, отсортированных от новых к старым
     */
    List<CertificateResponse> getCertificates(UUID practitionerId);

    /**
     * Загружает новый сертификат (диплом) практика.
     *
     * @param practitionerId идентификатор практика
     * @param name           название документа (например, «Диплом регрессолога»)
     * @param file           файл документа (PDF, JPEG, PNG)
     * @return данные сохранённого сертификата
     * @throws IOException если не удалось сохранить файл
     */
    CertificateResponse uploadCertificate(UUID practitionerId, String name, MultipartFile file) throws IOException;

    /**
     * Удаляет сертификат практика по идентификатору.
     * Если сертификат не принадлежит данному практику — выбрасывает ResourceNotFoundException.
     *
     * @param practitionerId идентификатор практика
     * @param certificateId  идентификатор сертификата
     */
    void deleteCertificate(UUID practitionerId, UUID certificateId);
}
