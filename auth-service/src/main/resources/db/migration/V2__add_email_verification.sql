ALTER TABLE auth_schema.users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE auth_schema.users SET email_verified = TRUE;

CREATE TABLE auth_schema.email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verification_user ON auth_schema.email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_hash ON auth_schema.email_verification_tokens(token_hash);
