CREATE TABLE auth_schema.push_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    installation_id VARCHAR(120) NOT NULL,
    push_token VARCHAR(255) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_push_device_platform CHECK (platform IN ('ANDROID', 'IOS')),
    CONSTRAINT uq_push_device_installation UNIQUE (user_id, installation_id),
    CONSTRAINT uq_push_device_token UNIQUE (push_token)
);
CREATE INDEX idx_push_devices_user_enabled ON auth_schema.push_devices (user_id) WHERE enabled = TRUE;
