ALTER TABLE sessions
    ADD COLUMN ai_summary TEXT,
    ADD COLUMN ai_summary_generated_at TIMESTAMPTZ;

ALTER TABLE clients
    ADD COLUMN ai_overall_summary TEXT,
    ADD COLUMN ai_overall_summary_generated_at TIMESTAMPTZ;
