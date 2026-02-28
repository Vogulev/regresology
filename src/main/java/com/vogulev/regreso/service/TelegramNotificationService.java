package com.vogulev.regreso.service;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Homework;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Reminder;

/**
 * Сервис для отправки уведомлений через Telegram-бота.
 * Обеспечивает рассылку напоминаний о сессиях, уведомлений
 * о домашних заданиях и алертов об неактивных клиентах.
 */
public interface TelegramNotificationService {

    /**
     * Отправляет напоминание о предстоящей сессии клиенту или практику.
     *
     * @param reminder данные напоминания (получатель, сессия, время)
     */
    void sendSessionReminder(Reminder reminder);

    /**
     * Отправляет клиенту уведомление о новом домашнем задании.
     *
     * @param client   клиент, которому назначено задание
     * @param homework данные домашнего задания
     */
    void sendHomeworkNotification(Client client, Homework homework);

    /**
     * Отправляет практику предупреждение о клиенте, который давно не был на сессии.
     *
     * @param practitioner практик, которому отправляется алерт
     * @param client       неактивный клиент
     * @param daysSince    количество дней с последней сессии клиента
     */
    void sendPractitionerInactiveClientAlert(Practitioner practitioner, Client client, long daysSince);
}
