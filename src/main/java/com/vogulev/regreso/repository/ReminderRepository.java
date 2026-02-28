package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с напоминаниями, отправляемыми клиентам.
 */
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    /**
     * Возвращает все напоминания с указанным статусом, запланированные к отправке до заданного момента времени.
     * Используется планировщиком для выборки напоминаний, готовых к отправке.
     *
     * @param status статус напоминания (например, PENDING)
     * @param before верхняя граница времени отправки (включительно)
     * @return список напоминаний, ожидающих отправки
     */
    List<Reminder> findByStatusAndSendAtBefore(Reminder.Status status, OffsetDateTime before);
}
