-- A non-reversible account fingerprint prevents old WebSockets from recreating erased data.
CREATE TABLE ai_schema.erased_accounts (
    account_key VARCHAR(64) PRIMARY KEY,
    erased_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
