CREATE TABLE emotion_schema.pmdata_stress_predictions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    feature_date DATE NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    feature_version VARCHAR(60) NOT NULL,
    stress_score INTEGER NOT NULL CHECK (stress_score BETWEEN 1 AND 5),
    relative_level VARCHAR(30) NOT NULL,
    confidence NUMERIC(6, 5) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    model_version VARCHAR(80) NOT NULL,
    alert_level VARCHAR(30) NOT NULL,
    notification_required BOOLEAN NOT NULL DEFAULT FALSE,
    notified_score INTEGER CHECK (notified_score BETWEEN 4 AND 5),
    notified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_pmdata_prediction_user_day_model
        UNIQUE (user_id, feature_date, model_version)
);

CREATE INDEX idx_pmdata_predictions_user_date
    ON emotion_schema.pmdata_stress_predictions (user_id, feature_date DESC);
