CREATE TABLE auth_schema.reminder_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    reminder_type VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    local_time TIME NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    last_sent_local_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_reminder_type CHECK (reminder_type IN ('DAILY_CHECK_IN', 'SELF_CARE')),
    CONSTRAINT uq_reminder_user_type UNIQUE (user_id, reminder_type)
);

CREATE INDEX idx_reminder_preferences_enabled ON auth_schema.reminder_preferences (enabled) WHERE enabled = TRUE;
