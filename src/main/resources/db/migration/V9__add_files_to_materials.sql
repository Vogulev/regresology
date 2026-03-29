ALTER TABLE practitioner_materials
    ADD COLUMN file_url TEXT,
    ADD COLUMN file_name VARCHAR(255),
    ADD COLUMN mime_type VARCHAR(255),
    ADD COLUMN file_size_bytes BIGINT;
