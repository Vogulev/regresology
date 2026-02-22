package com.vogulev.regreso.scheduler;

import com.vogulev.regreso.entity.Reminder;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.repository.ReminderRepository;
import com.vogulev.regreso.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final TelegramNotificationService telegramNotificationService;

    @Scheduled(cron = "${reminders.scheduler.cron:0 * * * * *}")
    @Transactional
    public void processReminders() {
        List<Reminder> pending = reminderRepository
                .findByStatusAndSendAtBefore(Reminder.Status.PENDING, OffsetDateTime.now());

        if (pending.isEmpty()) return;
        log.debug("Обработка {} ожидающих напоминаний", pending.size());

        for (Reminder reminder : pending) {
            try {
                telegramNotificationService.sendSessionReminder(reminder);
                reminder.setStatus(Reminder.Status.SENT);
                reminder.setSentAt(OffsetDateTime.now());
                updateSessionReminderFlags(reminder);
            } catch (Exception e) {
                log.error("Ошибка отправки напоминания id={}: {}", reminder.getId(), e.getMessage());
                reminder.setStatus(Reminder.Status.FAILED);
                reminder.setErrorMessage(e.getMessage());
            }
        }
    }

    private void updateSessionReminderFlags(Reminder reminder) {
        Session session = reminder.getSession();
        boolean is24h = reminder.getSendAt()
                .isBefore(session.getScheduledAt().minusHours(2));
        if (is24h) {
            session.setReminder24hSent(true);
        } else {
            session.setReminder1hSent(true);
        }
    }
}
