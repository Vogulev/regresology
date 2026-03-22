ALTER TABLE booking_settings
    ADD COLUMN availability_mode VARCHAR(20) DEFAULT 'DEFAULT',
    ADD COLUMN weekly_availability JSONB;
