ALTER TABLE auth_schema.notifications
    RENAME COLUMN content TO message;

ALTER TABLE auth_schema.notifications
    RENAME COLUMN notification_type TO type;

ALTER TABLE auth_schema.notifications
    ADD COLUMN source_event_id UUID;

ALTER TABLE auth_schema.notifications
    ALTER COLUMN title TYPE VARCHAR(160),
    ALTER COLUMN message TYPE VARCHAR(500),
    ALTER COLUMN type TYPE VARCHAR(40);

CREATE UNIQUE INDEX uq_notifications_source_user
    ON auth_schema.notifications (source_event_id, user_id)
    WHERE source_event_id IS NOT NULL;

CREATE INDEX idx_notifications_user_unread
    ON auth_schema.notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;

CREATE TABLE auth_schema.bookmarks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    target_type VARCHAR(20) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_bookmarks_target_type
        CHECK (target_type IN ('EXPERT', 'ASSESSMENT')),
    CONSTRAINT uq_bookmarks_user_target
        UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_bookmarks_user_created
    ON auth_schema.bookmarks (user_id, created_at DESC);
