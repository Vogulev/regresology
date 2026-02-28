package com.vogulev.regreso.service;

import java.util.UUID;

/**
 * Сервис для генерации ссылок на Telegram-бота.
 * Создаёт персональные ссылки для подключения клиентов
 * и практиков к Telegram-боту системы.
 */
public interface TelegramLinkService {

    /**
     * Генерирует ссылку для подключения клиента к Telegram-боту.
     *
     * @param clientId идентификатор клиента
     * @return ссылка для подключения клиента к боту
     */
    String generateClientLink(UUID clientId);

    /**
     * Генерирует ссылку для подключения практика к Telegram-боту.
     *
     * @param practitionerId идентификатор практика
     * @return ссылка для подключения практика к боту
     */
    String generatePractitionerLink(UUID practitionerId);
}
