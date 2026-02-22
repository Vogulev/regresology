package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.bot.RegresoBot;
import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Homework;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Reminder;
import com.vogulev.regreso.service.TelegramNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class TelegramNotificationServiceImpl implements TelegramNotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${telegram.bot.enabled:false}")
    private boolean botEnabled;

    @Autowired(required = false)
    private RegresoBot bot;

    @Override
    public void sendSessionReminder(Reminder reminder) {
        Long chatId = resolveRecipientChatId(reminder);
        if (!canSend(chatId)) return;

        String text = buildReminderText(reminder);
        send(chatId, text);
    }

    @Override
    public void sendHomeworkNotification(Client client, Homework homework) {
        if (!canSend(client.getTelegramChatId())) return;

        String practitionerName = homework.getPractitioner().getFirstName();
        String text = String.format(
                "📝 Новое задание от %s:\n%s\n\n%s\n\nКогда выполните — просто напишите ответ сюда",
                practitionerName,
                homework.getTitle(),
                homework.getDescription()
        );
        send(client.getTelegramChatId(), text);
    }

    @Override
    public void sendPractitionerInactiveClientAlert(Practitioner practitioner, Client client, long daysSince) {
        if (!canSend(practitioner.getTelegramChatId())) return;

        String text = String.format(
                "💤 %s не был на сессии %d дней\nВозможно, стоит написать клиенту",
                client.getFullName(),
                daysSince
        );
        send(practitioner.getTelegramChatId(), text);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private boolean canSend(Long chatId) {
        if (!botEnabled || bot == null) {
            log.debug("Telegram bot disabled — пропускаем отправку");
            return false;
        }
        if (chatId == null) {
            log.debug("chat_id не задан — пропускаем отправку");
            return false;
        }
        return true;
    }

    private Long resolveRecipientChatId(Reminder reminder) {
        if (reminder.getRecipientType() == Reminder.RecipientType.CLIENT) {
            return reminder.getSession().getClient().getTelegramChatId();
        } else {
            return reminder.getSession().getPractitioner().getTelegramChatId();
        }
    }

    private String buildReminderText(Reminder reminder) {
        var session = reminder.getSession();
        var practitioner = session.getPractitioner();
        var practitionerName = practitioner.getFirstName();

        ZoneId zone = resolveZone(practitioner.getTimezone());
        var scheduledLocal = session.getScheduledAt().atZoneSameInstant(zone);
        boolean is24h = reminder.getSendAt()
                .isBefore(session.getScheduledAt().minusHours(2));

        if (is24h) {
            return String.format(
                    "Напоминание: завтра у вас сессия с %s\n🗓 %s в %s\nПродолжительность: %d мин",
                    practitionerName,
                    scheduledLocal.format(DATE_FMT),
                    scheduledLocal.format(TIME_FMT),
                    session.getDurationMin() != null ? session.getDurationMin() : 120
            );
        } else {
            return String.format(
                    "Напоминание: через час ваша сессия с %s\n🕐 Начало в %s",
                    practitionerName,
                    scheduledLocal.format(TIME_FMT)
            );
        }
    }

    private void send(Long chatId, String text) {
        try {
            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка отправки Telegram сообщения chatId={}: {}", chatId, e.getMessage());
            throw new RuntimeException("Telegram send failed: " + e.getMessage(), e);
        }
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) return ZoneId.of("Europe/Moscow");
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Europe/Moscow");
        }
    }
}
