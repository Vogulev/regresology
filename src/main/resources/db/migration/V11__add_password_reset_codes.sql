CREATE TABLE password_reset_codes (
    id UUID PRIMARY KEY,
    practitioner_id UUID NOT NULL UNIQUE REFERENCES practitioners(id) ON DELETE CASCADE,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ
);

CREATE INDEX idx_password_reset_codes_expires_at ON password_reset_codes(expires_at);
