ALTER TABLE booking_schema.conversations
    ADD COLUMN user_id UUID,
    ADD COLUMN expert_user_id UUID;

UPDATE booking_schema.conversations AS conversation
SET user_id = booking.user_id,
    expert_user_id = booking.expert_user_id
FROM booking_schema.bookings AS booking
WHERE conversation.booking_id = booking.id;

ALTER TABLE booking_schema.conversations
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN expert_user_id SET NOT NULL,
    ALTER COLUMN booking_id DROP NOT NULL,
    DROP CONSTRAINT uq_conversations_booking;

CREATE UNIQUE INDEX uq_conversations_active_participants
    ON booking_schema.conversations (user_id, expert_user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_conversations_user_history
    ON booking_schema.conversations (user_id, opened_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_conversations_expert_history
    ON booking_schema.conversations (expert_user_id, opened_at DESC, id DESC)
    WHERE deleted_at IS NULL;
