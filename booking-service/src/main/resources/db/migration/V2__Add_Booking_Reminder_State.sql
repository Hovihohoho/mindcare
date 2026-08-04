ALTER TABLE booking_schema.bookings
    ADD COLUMN reminder_sent_at TIMESTAMPTZ;

CREATE INDEX idx_bookings_reminder_pending
    ON booking_schema.bookings (status, reminder_sent_at, schedule_id)
    WHERE deleted_at IS NULL
      AND status = 'CONFIRMED'
      AND reminder_sent_at IS NULL;
