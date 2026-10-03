CREATE TABLE auth_schema.expo_push_receipts (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES auth_schema.push_devices(id) ON DELETE CASCADE,
    ticket_id VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL,
    checked_at TIMESTAMPTZ,
    CONSTRAINT ck_expo_push_receipt_status
        CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED'))
);

CREATE INDEX idx_expo_push_receipts_pending
    ON auth_schema.expo_push_receipts (created_at)
    WHERE status = 'PENDING';
