CREATE INDEX IF NOT EXISTS idx_session_media_session_created_at
    ON session_media (session_id, created_at);
