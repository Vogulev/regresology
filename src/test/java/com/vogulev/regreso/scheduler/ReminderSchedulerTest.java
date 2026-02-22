package com.vogulev.regreso.scheduler;

import com.vogulev.regreso.entity.Client;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.Reminder;
import com.vogulev.regreso.entity.Session;
import com.vogulev.regreso.repository.ReminderRepository;
import com.vogulev.regreso.service.TelegramNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReminderScheduler — юнит-тесты")
class ReminderSchedulerTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @InjectMocks
    private ReminderScheduler scheduler;

    private Reminder pendingReminder;

    @BeforeEach
    void setUp() {
        Practitioner practitioner = Practitioner.builder()
                .id(UUID.randomUUID())
                .firstName("Анна")
                .timezone("Europe/Moscow")
                .telegramChatId(111L)
                .build();

        Client client = Client.builder()
                .id(UUID.randomUUID())
                .firstName("Иван")
                .telegramChatId(999L)
                .practitioner(practitioner)
                .build();

        OffsetDateTime scheduledAt = OffsetDateTime.now().plusHours(23);

        Session session = Session.builder()
                .id(UUID.randomUUID())
                .practitioner(practitioner)
                .client(client)
                .scheduledAt(scheduledAt)
                .durationMin(90)
                .build();

        pendingReminder = Reminder.builder()
                .id(UUID.randomUUID())
                .session(session)
                .recipientType(Reminder.RecipientType.CLIENT)
                .channel(Reminder.Channel.TELEGRAM)
                .sendAt(scheduledAt.minusHours(24))
                .build();
    }

    @Test
    @DisplayName("PENDING reminder → отправляется → статус SENT")
    void processReminders_pending_setsStatusSent() {
        when(reminderRepository.findByStatusAndSendAtBefore(eq(Reminder.Status.PENDING), any()))
                .thenReturn(List.of(pendingReminder));

        scheduler.processReminders();

        verify(telegramNotificationService).sendSessionReminder(pendingReminder);
        assertThat(pendingReminder.getStatus()).isEqualTo(Reminder.Status.SENT);
        assertThat(pendingReminder.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("ошибка при отправке → статус FAILED, errorMessage заполнен")
    void processReminders_onException_setsStatusFailed() {
        when(reminderRepository.findByStatusAndSendAtBefore(eq(Reminder.Status.PENDING), any()))
                .thenReturn(List.of(pendingReminder));
        doThrow(new RuntimeException("Telegram недоступен"))
                .when(telegramNotificationService).sendSessionReminder(pendingReminder);

        scheduler.processReminders();

        assertThat(pendingReminder.getStatus()).isEqualTo(Reminder.Status.FAILED);
        assertThat(pendingReminder.getErrorMessage()).contains("Telegram недоступен");
    }

    @Test
    @DisplayName("нет PENDING напоминаний → TelegramNotificationService не вызывается")
    void processReminders_noPending_doesNothing() {
        when(reminderRepository.findByStatusAndSendAtBefore(eq(Reminder.Status.PENDING), any()))
                .thenReturn(List.of());

        scheduler.processReminders();

        verifyNoInteractions(telegramNotificationService);
    }

    @Test
    @DisplayName("несколько напоминаний — одна ошибка не ломает остальные")
    void processReminders_oneFailure_othersStillProcessed() {
        Practitioner p = Practitioner.builder().id(UUID.randomUUID()).firstName("П").timezone("UTC").build();
        Client c = Client.builder().id(UUID.randomUUID()).firstName("К").practitioner(p).build();
        OffsetDateTime at = OffsetDateTime.now().plusHours(1);
        Session s = Session.builder().id(UUID.randomUUID()).practitioner(p).client(c).scheduledAt(at).durationMin(60).build();

        Reminder bad = Reminder.builder().id(UUID.randomUUID()).session(s)
                .recipientType(Reminder.RecipientType.CLIENT).channel(Reminder.Channel.TELEGRAM)
                .sendAt(at.minusHours(24)).build();
        Reminder good = Reminder.builder().id(UUID.randomUUID()).session(s)
                .recipientType(Reminder.RecipientType.CLIENT).channel(Reminder.Channel.TELEGRAM)
                .sendAt(at.minusHours(24)).build();

        when(reminderRepository.findByStatusAndSendAtBefore(eq(Reminder.Status.PENDING), any()))
                .thenReturn(List.of(bad, good));
        doThrow(new RuntimeException("ошибка")).when(telegramNotificationService).sendSessionReminder(bad);

        scheduler.processReminders();

        assertThat(bad.getStatus()).isEqualTo(Reminder.Status.FAILED);
        assertThat(good.getStatus()).isEqualTo(Reminder.Status.SENT);
    }

    @Test
    @DisplayName("отправка 24h напоминания → session.reminder24hSent = true")
    void processReminders_24h_setsReminder24hFlag() {
        OffsetDateTime scheduledAt = OffsetDateTime.now().plusHours(23);
        pendingReminder.setSendAt(scheduledAt.minusHours(24)); // 24h before
        pendingReminder.getSession().setScheduledAt(scheduledAt);

        when(reminderRepository.findByStatusAndSendAtBefore(eq(Reminder.Status.PENDING), any()))
                .thenReturn(List.of(pendingReminder));

        scheduler.processReminders();

        assertThat(pendingReminder.getSession().getReminder24hSent()).isTrue();
    }

    @Test
    @DisplayName("клиент без telegram_chat_id — TelegramNotificationService всё равно вызывается (он сам проверит)")
    void processReminders_clientWithoutChatId_serviceStillCalled() {
        pendingReminder.getSession().getClient().setTelegramChatId(null);

        when(reminderRepository.findByStatusAndSendAtBefore(eq(Reminder.Status.PENDING), any()))
                .thenReturn(List.of(pendingReminder));

        scheduler.processReminders();

        // Сервис вызывается — он сам решает, пропускать или нет
        verify(telegramNotificationService).sendSessionReminder(pendingReminder);
        assertThat(pendingReminder.getStatus()).isEqualTo(Reminder.Status.SENT);
    }
}
