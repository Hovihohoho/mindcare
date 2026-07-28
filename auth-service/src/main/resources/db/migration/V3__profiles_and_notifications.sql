ALTER TABLE auth_schema.users
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN birth_date DATE,
    ADD COLUMN gender VARCHAR(30),
    ADD COLUMN address VARCHAR(500),
    ADD COLUMN bio TEXT,
    ADD COLUMN headline VARCHAR(255),
    ADD COLUMN specialties TEXT,
    ADD COLUMN years_of_experience INTEGER,
    ADD COLUMN consultation_fee NUMERIC(12, 2),
    ADD COLUMN workplace VARCHAR(255),
    ADD COLUMN education TEXT;

CREATE TABLE auth_schema.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    action_url VARCHAR(500),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_created
    ON auth_schema.notifications(user_id, created_at DESC);
