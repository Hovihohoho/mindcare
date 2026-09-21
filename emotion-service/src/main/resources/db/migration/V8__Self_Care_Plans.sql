CREATE TABLE emotion_schema.self_care_plans (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    goal VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_self_care_plan_user UNIQUE (user_id),
    CONSTRAINT ck_self_care_plan_goal CHECK (goal IN ('REDUCE_STRESS', 'IMPROVE_SLEEP', 'MANAGE_ANXIETY', 'BUILD_BALANCE'))
);

CREATE TABLE emotion_schema.self_care_activities (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES emotion_schema.self_care_plans(id) ON DELETE CASCADE,
    activity_code VARCHAR(50) NOT NULL,
    title VARCHAR(160) NOT NULL,
    target_per_week INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_self_care_activity_target CHECK (target_per_week BETWEEN 1 AND 7),
    CONSTRAINT uq_self_care_activity_code UNIQUE (plan_id, activity_code),
    CONSTRAINT uq_self_care_activity_order UNIQUE (plan_id, display_order)
);

CREATE TABLE emotion_schema.self_care_completions (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES emotion_schema.self_care_activities(id) ON DELETE CASCADE,
    completed_on DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_self_care_completion_day UNIQUE (activity_id, completed_on)
);

CREATE INDEX idx_self_care_completion_activity_date
    ON emotion_schema.self_care_completions (activity_id, completed_on DESC);
