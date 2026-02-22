package com.vogulev.regreso.service;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Homework;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Reminder;

public interface TelegramNotificationService {

    void sendSessionReminder(Reminder reminder);

    void sendHomeworkNotification(Client client, Homework homework);

    void sendPractitionerInactiveClientAlert(Practitioner practitioner, Client client, long daysSince);
}
