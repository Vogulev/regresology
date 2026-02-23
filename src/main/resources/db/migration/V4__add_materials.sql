CREATE TABLE practitioner_materials (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practitioner_id UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    material_type   VARCHAR(50) NOT NULL,
    content         TEXT,
    is_archived     BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE homework
    ADD COLUMN material_id UUID REFERENCES practitioner_materials(id) ON DELETE SET NULL;

CREATE INDEX idx_materials_practitioner ON practitioner_materials(practitioner_id, is_archived);
