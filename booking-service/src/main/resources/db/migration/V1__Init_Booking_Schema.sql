CREATE SCHEMA IF NOT EXISTS booking_schema;

CREATE TABLE booking_schema.expert_schedules (
    id UUID PRIMARY KEY,
    expert_user_id UUID NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    hold_expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_expert_schedules_range
        CHECK (start_at < end_at),
    CONSTRAINT ck_expert_schedules_status
        CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED', 'CANCELLED')),
    CONSTRAINT ck_expert_schedules_hold
        CHECK (
            (status = 'HELD' AND hold_expires_at IS NOT NULL)
            OR
            (status <> 'HELD' AND hold_expires_at IS NULL)
        )
);

CREATE UNIQUE INDEX uq_expert_schedules_exact_active_slot
    ON booking_schema.expert_schedules (expert_user_id, start_at, end_at)
    WHERE deleted_at IS NULL AND status <> 'CANCELLED';

CREATE INDEX idx_expert_schedules_available_start
    ON booking_schema.expert_schedules (expert_user_id, start_at, id)
    WHERE deleted_at IS NULL AND status = 'AVAILABLE';

CREATE TABLE booking_schema.bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    expert_user_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    note TEXT,
    price NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    cancellation_reason TEXT,
    canceled_by VARCHAR(20),
    expires_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    cancellation_requested_at TIMESTAMPTZ,
    cancellation_review_deadline TIMESTAMPTZ,
    cancellation_decided_at TIMESTAMPTZ,
    cancellation_decision_reason TEXT,
    completed_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_bookings_schedule
        FOREIGN KEY (schedule_id)
        REFERENCES booking_schema.expert_schedules (id),
    CONSTRAINT uq_bookings_user_idempotency
        UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_bookings_status
        CHECK (
            status IN (
                'PAYMENT_PENDING',
                'CONFIRMED',
                'CANCELLATION_PENDING',
                'CANCELED',
                'COMPLETED',
                'USER_NO_SHOW',
                'EXPERT_NO_SHOW',
                'PAYMENT_FAILED',
                'EXPIRED'
            )
        ),
    CONSTRAINT ck_bookings_payment_status
        CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED')),
    CONSTRAINT ck_bookings_price
        CHECK (price >= 0),
    CONSTRAINT ck_bookings_currency
        CHECK (currency = UPPER(currency) AND char_length(currency) = 3),
    CONSTRAINT ck_bookings_canceled_by
        CHECK (canceled_by IS NULL OR canceled_by IN ('USER', 'EXPERT', 'ADMIN', 'SYSTEM')),
    CONSTRAINT ck_bookings_expiry
        CHECK (expires_at IS NULL OR created_at < expires_at),
    CONSTRAINT ck_bookings_confirmation
        CHECK (
            (status IN ('CONFIRMED', 'CANCELLATION_PENDING', 'COMPLETED', 'USER_NO_SHOW', 'EXPERT_NO_SHOW')
                AND confirmed_at IS NOT NULL)
            OR
            (status NOT IN ('CONFIRMED', 'CANCELLATION_PENDING', 'COMPLETED', 'USER_NO_SHOW', 'EXPERT_NO_SHOW'))
        ),
    CONSTRAINT ck_bookings_cancellation_request
        CHECK (
            (status = 'CANCELLATION_PENDING'
                AND cancellation_requested_at IS NOT NULL
                AND cancellation_review_deadline IS NOT NULL)
            OR
            status <> 'CANCELLATION_PENDING'
        )
);

CREATE UNIQUE INDEX uq_bookings_active_schedule
    ON booking_schema.bookings (schedule_id)
    WHERE deleted_at IS NULL
        AND status IN ('PAYMENT_PENDING', 'CONFIRMED', 'CANCELLATION_PENDING');

CREATE INDEX idx_bookings_user_history
    ON booking_schema.bookings (user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_bookings_expert_history
    ON booking_schema.bookings (expert_user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE booking_schema.conversations (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_conversations_booking UNIQUE (booking_id),
    CONSTRAINT fk_conversations_booking
        FOREIGN KEY (booking_id)
        REFERENCES booking_schema.bookings (id),
    CONSTRAINT ck_conversations_time
        CHECK (closed_at IS NULL OR opened_at <= closed_at)
);

CREATE TABLE booking_schema.messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    content TEXT,
    attachment_url VARCHAR(1000),
    is_read BOOLEAN NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES booking_schema.conversations (id),
    CONSTRAINT ck_messages_type
        CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')),
    CONSTRAINT ck_messages_payload
        CHECK (
            (message_type = 'TEXT' AND content IS NOT NULL AND btrim(content) <> '')
            OR
            (message_type IN ('IMAGE', 'FILE') AND attachment_url IS NOT NULL)
        ),
    CONSTRAINT ck_messages_read_at
        CHECK (
            (is_read = FALSE AND read_at IS NULL)
            OR
            (is_read = TRUE AND read_at IS NOT NULL)
        )
);

CREATE INDEX idx_messages_conversation_history
    ON booking_schema.messages (conversation_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE booking_schema.expert_reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    user_id UUID NOT NULL,
    expert_user_id UUID NOT NULL,
    rating SMALLINT NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_expert_reviews_booking UNIQUE (booking_id),
    CONSTRAINT fk_expert_reviews_booking
        FOREIGN KEY (booking_id)
        REFERENCES booking_schema.bookings (id),
    CONSTRAINT ck_expert_reviews_rating
        CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_expert_reviews_expert_created
    ON booking_schema.expert_reviews (expert_user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE booking_schema.consultation_notes (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    expert_user_id UUID NOT NULL,
    observation TEXT,
    recommendation TEXT NOT NULL,
    recovery_plan TEXT,
    visible_to_user BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_consultation_notes_booking UNIQUE (booking_id),
    CONSTRAINT fk_consultation_notes_booking
        FOREIGN KEY (booking_id)
        REFERENCES booking_schema.bookings (id),
    CONSTRAINT ck_consultation_notes_recommendation
        CHECK (btrim(recommendation) <> '')
);

CREATE TABLE booking_schema.payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    provider_order_id VARCHAR(255) NOT NULL,
    transaction_code VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    checkout_url VARCHAR(2000),
    expires_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_code VARCHAR(100),
    late_success BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id)
        REFERENCES booking_schema.bookings (id),
    CONSTRAINT uq_payments_provider_order UNIQUE (provider_order_id),
    CONSTRAINT uq_payments_idempotency UNIQUE (user_id, booking_id, idempotency_key),
    CONSTRAINT ck_payments_amount
        CHECK (amount >= 0),
    CONSTRAINT ck_payments_currency
        CHECK (currency = UPPER(currency) AND char_length(currency) = 3),
    CONSTRAINT ck_payments_method
        CHECK (payment_method IN ('VNPAY', 'MOMO', 'ZALOPAY', 'BANK_TRANSFER')),
    CONSTRAINT ck_payments_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED'))
);

CREATE UNIQUE INDEX uq_payments_one_pending_attempt
    ON booking_schema.payments (booking_id)
    WHERE deleted_at IS NULL AND status = 'PENDING';

CREATE UNIQUE INDEX uq_payments_transaction_code
    ON booking_schema.payments (transaction_code)
    WHERE transaction_code IS NOT NULL;

CREATE INDEX idx_payments_booking_created
    ON booking_schema.payments (booking_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE booking_schema.payment_refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_refund_id VARCHAR(255),
    failure_code VARCHAR(100),
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_payment_refunds_payment UNIQUE (payment_id),
    CONSTRAINT fk_payment_refunds_payment
        FOREIGN KEY (payment_id)
        REFERENCES booking_schema.payments (id),
    CONSTRAINT fk_payment_refunds_booking
        FOREIGN KEY (booking_id)
        REFERENCES booking_schema.bookings (id),
    CONSTRAINT ck_payment_refunds_amount
        CHECK (amount > 0),
    CONSTRAINT ck_payment_refunds_currency
        CHECK (currency = UPPER(currency) AND char_length(currency) = 3),
    CONSTRAINT ck_payment_refunds_reason
        CHECK (
            reason IN (
                'USER_CANCELED_IN_POLICY',
                'EXPERT_CANCELED',
                'SYSTEM_CANCELED',
                'EXPERT_NO_SHOW',
                'LATE_PAYMENT_SUCCESS',
                'DUPLICATE_PAYMENT',
                'ADMIN_APPROVED'
            )
        ),
    CONSTRAINT ck_payment_refunds_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_payment_refunds_pending
    ON booking_schema.payment_refunds (status, requested_at, id)
    WHERE deleted_at IS NULL AND status IN ('PENDING', 'FAILED');

CREATE TABLE booking_schema.payment_webhook_receipts (
    id UUID PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    signature_verified BOOLEAN NOT NULL,
    processing_status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_payment_webhook_provider_event
        UNIQUE (provider, provider_event_id),
    CONSTRAINT ck_payment_webhook_provider
        CHECK (provider IN ('VNPAY', 'MOMO', 'ZALOPAY', 'BANK_TRANSFER')),
    CONSTRAINT ck_payment_webhook_status
        CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'REJECTED'))
);

CREATE TABLE booking_schema.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL,
    next_attempt_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_outbox_event_version
        CHECK (event_version > 0),
    CONSTRAINT ck_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_outbox_pending
    ON booking_schema.outbox_events (status, next_attempt_at, created_at, id)
    WHERE deleted_at IS NULL AND status IN ('PENDING', 'FAILED');
