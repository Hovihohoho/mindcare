CREATE TABLE auth_schema.notification_delivery_jobs (
    notification_id UUID PRIMARY KEY REFERENCES auth_schema.notifications(id) ON DELETE CASCADE,
    push_requested BOOLEAN NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_until TIMESTAMPTZ
);
CREATE INDEX idx_notification_delivery_due ON auth_schema.notification_delivery_jobs(next_attempt_at);
