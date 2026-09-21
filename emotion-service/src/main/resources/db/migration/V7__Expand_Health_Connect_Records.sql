ALTER TABLE emotion_schema.health_metrics
    ALTER COLUMN metric_value DROP NOT NULL,
    ADD COLUMN start_time TIMESTAMPTZ,
    ADD COLUMN end_time TIMESTAMPTZ,
    ADD COLUMN source_name VARCHAR(255),
    ADD COLUMN data_origin VARCHAR(255),
    ADD COLUMN source_last_modified_at TIMESTAMPTZ,
    ADD COLUMN record_details JSONB;

DROP INDEX emotion_schema.uq_health_metric_external_sample;

CREATE UNIQUE INDEX uq_health_metric_external_sample
    ON emotion_schema.health_metrics (user_id, source_type, external_sample_id)
    WHERE external_sample_id IS NOT NULL;

CREATE INDEX idx_health_metric_daily_aggregation
    ON emotion_schema.health_metrics (user_id, metric_type, recorded_at)
    WHERE deleted_at IS NULL;

