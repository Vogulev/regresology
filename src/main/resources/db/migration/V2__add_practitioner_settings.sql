-- Количество дней неактивности клиента для уведомления практика (0 = отключено)
ALTER TABLE practitioners
    ADD COLUMN inactive_client_reminder_days INTEGER DEFAULT 0;
