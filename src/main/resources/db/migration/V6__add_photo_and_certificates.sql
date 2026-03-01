ALTER TABLE practitioners ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS certificates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practitioner_id UUID         NOT NULL REFERENCES practitioners (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_certificates_practitioner_id ON certificates (practitioner_id);
