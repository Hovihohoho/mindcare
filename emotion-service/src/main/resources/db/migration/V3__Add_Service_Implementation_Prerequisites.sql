ALTER TABLE emotion_schema.health_metrics
    ADD COLUMN external_sample_id VARCHAR(255);

CREATE UNIQUE INDEX uq_health_metric_external_sample
    ON emotion_schema.health_metrics (user_id, source_type, external_sample_id)
    WHERE external_sample_id IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE emotion_schema.health_metric_sync_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_health_metric_sync_request UNIQUE (user_id, source_type, idempotency_key)
);

ALTER TABLE emotion_schema.assessments
    DROP CONSTRAINT IF EXISTS assessments_code_key,
    ADD COLUMN assessment_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD CONSTRAINT ck_assessment_version_positive CHECK (assessment_version > 0),
    ADD CONSTRAINT ck_assessment_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    ADD CONSTRAINT uq_assessment_code_version UNIQUE (code, assessment_version);

CREATE UNIQUE INDEX uq_assessment_one_published_version
    ON emotion_schema.assessments (code)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

ALTER TABLE emotion_schema.answer_options
    ADD COLUMN order_index INTEGER;

WITH ranked_options AS (
    SELECT id,
           (ROW_NUMBER() OVER (PARTITION BY question_id ORDER BY created_at, id) - 1)::INTEGER AS generated_order
    FROM emotion_schema.answer_options
)
UPDATE emotion_schema.answer_options AS answer_option
SET order_index = ranked_options.generated_order
FROM ranked_options
WHERE answer_option.id = ranked_options.id;

ALTER TABLE emotion_schema.answer_options
    ALTER COLUMN order_index SET NOT NULL,
    ADD CONSTRAINT ck_answer_option_order_non_negative CHECK (order_index >= 0);

CREATE UNIQUE INDEX uq_active_question_order
    ON emotion_schema.questions (assessment_id, order_index)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_active_answer_option_order
    ON emotion_schema.answer_options (question_id, order_index)
    WHERE deleted_at IS NULL;

ALTER TABLE emotion_schema.assessment_results
    ADD COLUMN assessment_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN scoring_rule_version VARCHAR(50) NOT NULL DEFAULT 'legacy-v1',
    ADD COLUMN idempotency_key VARCHAR(255),
    ADD COLUMN submission_hash VARCHAR(64),
    ADD COLUMN screening_notice TEXT NOT NULL DEFAULT 'Kết quả chỉ mang tính sàng lọc và hỗ trợ, không phải chẩn đoán y khoa.',
    ADD COLUMN recommendations JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE UNIQUE INDEX uq_assessment_result_idempotency
    ON emotion_schema.assessment_results (user_id, assessment_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE emotion_schema.psychological_alert_logs
    ADD COLUMN rule_version VARCHAR(50) NOT NULL DEFAULT 'risk-v1',
    ADD COLUMN reason_code VARCHAR(100) NOT NULL DEFAULT 'LEGACY_ALERT',
    ADD COLUMN source_result_id UUID,
    ADD COLUMN deduplication_key VARCHAR(255),
    ADD COLUMN notified_at TIMESTAMPTZ,
    ADD CONSTRAINT fk_alert_source_result
        FOREIGN KEY (source_result_id) REFERENCES emotion_schema.assessment_results(id);

CREATE INDEX idx_alert_deduplication
    ON emotion_schema.psychological_alert_logs (user_id, deduplication_key, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_journal_active_history
    ON emotion_schema.emotion_journals (user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_health_metric_active_history
    ON emotion_schema.health_metrics (user_id, metric_type, recorded_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_assessment_result_active_history
    ON emotion_schema.assessment_results (user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
