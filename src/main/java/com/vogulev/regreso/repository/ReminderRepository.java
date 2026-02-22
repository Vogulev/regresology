package com.vogulev.regreso.repository;

import com.vogulev.regreso.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByStatusAndSendAtBefore(Reminder.Status status, OffsetDateTime before);
}
