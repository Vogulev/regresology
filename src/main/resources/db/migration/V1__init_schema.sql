CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Регрессологи (аккаунты)
CREATE TABLE practitioners (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password_hash               VARCHAR(255) NOT NULL,
    first_name                  VARCHAR(100) NOT NULL,
    last_name                   VARCHAR(100),
    phone                       VARCHAR(20),
    bio                         TEXT,
    timezone                    VARCHAR(50) DEFAULT 'Europe/Moscow',
    telegram_chat_id            BIGINT,
    default_session_duration_min INTEGER DEFAULT 120,
    plan                        VARCHAR(20) DEFAULT 'free',
    plan_expires_at             TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ DEFAULT now(),
    updated_at                  TIMESTAMPTZ DEFAULT now()
);

-- Клиенты
CREATE TABLE clients (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practitioner_id         UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100),
    phone                   VARCHAR(20),
    email                   VARCHAR(255),
    birth_date              DATE,
    telegram_username       VARCHAR(100),
    telegram_chat_id        BIGINT,
    initial_request         TEXT,
    presenting_issues       TEXT[],
    has_contraindications   BOOLEAN DEFAULT false,
    contraindications_notes TEXT,
    intake_form_completed   BOOLEAN DEFAULT false,
    overall_progress        TEXT,
    general_notes           TEXT,
    is_archived             BOOLEAN DEFAULT false,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now()
);

-- Сессии (протокол по 7 шагам)
CREATE TABLE sessions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practitioner_id         UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    client_id               UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    scheduled_at            TIMESTAMPTZ NOT NULL,
    duration_min            INTEGER DEFAULT 120,
    status                  VARCHAR(20) DEFAULT 'SCHEDULED',
    session_number          INTEGER,
    -- [1] Перед сессией
    pre_session_request     TEXT,
    pre_session_state       TEXT,
    pre_session_score       SMALLINT CHECK (pre_session_score BETWEEN 1 AND 10),
    -- [2] Введение в транс
    induction_method        VARCHAR(100),
    trance_depth            VARCHAR(20),
    induction_notes         TEXT,
    -- [3] Куда ушёл клиент
    regression_target       VARCHAR(50),
    regression_period       VARCHAR(255),
    regression_setting      TEXT,
    -- [4] Ключевые сцены
    key_scenes              TEXT,
    key_emotions            TEXT[],
    key_insights            TEXT,
    symbolic_images         TEXT,
    -- [5] Проработка
    blocks_released         TEXT,
    healing_occurred        BOOLEAN DEFAULT false,
    healing_notes           TEXT,
    -- [6] Выход
    post_session_state      TEXT,
    post_session_score      SMALLINT CHECK (post_session_score BETWEEN 1 AND 10),
    integration_notes       TEXT,
    -- [7] Итог практика (приватные заметки — клиенту не показывать!)
    practitioner_notes      TEXT,
    next_session_plan       TEXT,
    -- Финансы
    price                   DECIMAL(10,2),
    is_paid                 BOOLEAN DEFAULT false,
    -- Напоминания
    reminder_24h_sent       BOOLEAN DEFAULT false,
    reminder_1h_sent        BOOLEAN DEFAULT false,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now()
);

-- Медиафайлы сессии (аудио, фото)
CREATE TABLE session_media (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    media_type      VARCHAR(20) NOT NULL,
    file_key        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    file_size_bytes INTEGER,
    duration_sec    INTEGER,
    caption         TEXT,
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- Сквозные темы клиента
CREATE TABLE client_themes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    practitioner_id UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    first_seen_at   TIMESTAMPTZ,
    is_resolved     BOOLEAN DEFAULT false,
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- Связь тема <-> сессии
CREATE TABLE theme_sessions (
    theme_id    UUID NOT NULL REFERENCES client_themes(id) ON DELETE CASCADE,
    session_id  UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    notes       TEXT,
    PRIMARY KEY (theme_id, session_id)
);

-- Домашние задания
CREATE TABLE homework (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID REFERENCES sessions(id) ON DELETE SET NULL,
    client_id       UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    practitioner_id UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL,
    homework_type   VARCHAR(50),
    due_date        DATE,
    status          VARCHAR(20) DEFAULT 'ASSIGNED',
    client_response TEXT,
    responded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- Настройки онлайн-записи
CREATE TABLE booking_settings (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practitioner_id      UUID NOT NULL UNIQUE REFERENCES practitioners(id) ON DELETE CASCADE,
    is_enabled           BOOLEAN DEFAULT false,
    slug                 VARCHAR(100) UNIQUE,
    default_duration_min INTEGER DEFAULT 120,
    buffer_min           INTEGER DEFAULT 30,
    advance_days         INTEGER DEFAULT 30,
    require_intake_form  BOOLEAN DEFAULT true,
    services             JSONB,
    welcome_message      TEXT,
    updated_at           TIMESTAMPTZ DEFAULT now()
);

-- Очередь напоминаний
CREATE TABLE reminders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    recipient_type  VARCHAR(20) NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    send_at         TIMESTAMPTZ NOT NULL,
    sent_at         TIMESTAMPTZ,
    status          VARCHAR(20) DEFAULT 'PENDING',
    error_message   TEXT,
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- Подписки / оплата
CREATE TABLE subscriptions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practitioner_id  UUID NOT NULL REFERENCES practitioners(id) ON DELETE CASCADE,
    plan             VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    started_at       TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    payment_provider VARCHAR(50),
    external_id      VARCHAR(255),
    amount           DECIMAL(10,2),
    currency         VARCHAR(3) DEFAULT 'RUB',
    created_at       TIMESTAMPTZ DEFAULT now()
);

-- Индексы
CREATE INDEX idx_clients_practitioner  ON clients(practitioner_id);
CREATE INDEX idx_clients_archived      ON clients(practitioner_id, is_archived);
CREATE INDEX idx_sessions_practitioner ON sessions(practitioner_id);
CREATE INDEX idx_sessions_client       ON sessions(client_id);
CREATE INDEX idx_sessions_scheduled    ON sessions(practitioner_id, scheduled_at);
CREATE INDEX idx_sessions_status       ON sessions(practitioner_id, status);
CREATE INDEX idx_themes_client         ON client_themes(client_id);
CREATE INDEX idx_homework_client       ON homework(client_id);
CREATE INDEX idx_reminders_pending     ON reminders(send_at, status) WHERE status = 'PENDING';
CREATE INDEX idx_booking_slug          ON booking_settings(slug);
