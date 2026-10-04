CREATE TABLE auth_schema.account_deletion_jobs (
    user_id UUID PRIMARY KEY REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP + INTERVAL '2 minutes',
    lease_until TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    emotion_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ai_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_account_deletion_due ON auth_schema.account_deletion_jobs(next_attempt_at);
