ALTER TABLE emotion_schema.emotion_journals
    ADD COLUMN entry_source VARCHAR(40) NOT NULL DEFAULT 'USER_DIRECT';

ALTER TABLE emotion_schema.emotion_journals
    ADD CONSTRAINT ck_emotion_journal_entry_source
        CHECK (entry_source IN ('USER_DIRECT', 'MORNING_WELLBEING_PROMPT'));

CREATE INDEX idx_emotion_journals_user_source_created
    ON emotion_schema.emotion_journals (user_id, entry_source, created_at DESC)
    WHERE deleted_at IS NULL;
