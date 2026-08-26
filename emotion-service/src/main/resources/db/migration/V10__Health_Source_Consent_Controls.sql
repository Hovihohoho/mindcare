CREATE TABLE emotion_schema.health_source_consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_health_source_consent UNIQUE (user_id, source_type)
);

CREATE INDEX idx_health_source_consent_user
    ON emotion_schema.health_source_consents (user_id, source_type);
