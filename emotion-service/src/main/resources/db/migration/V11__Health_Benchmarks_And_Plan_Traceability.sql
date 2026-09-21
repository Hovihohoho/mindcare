ALTER TABLE emotion_schema.self_care_plans
    ADD COLUMN template_code VARCHAR(60),
    ADD COLUMN template_version VARCHAR(30),
    ADD COLUMN source_url VARCHAR(500);

ALTER TABLE emotion_schema.psychological_alert_logs
    ADD COLUMN alert_category VARCHAR(30) NOT NULL DEFAULT 'PSYCHOLOGICAL',
    ADD COLUMN benchmark_policy_key VARCHAR(100),
    ADD COLUMN benchmark_policy_version VARCHAR(30),
    ADD COLUMN benchmark_source_url VARCHAR(500),
    ADD COLUMN metric_type VARCHAR(50),
    ADD COLUMN observed_value NUMERIC(12, 2),
    ADD COLUMN observed_unit VARCHAR(20),
    ADD COLUMN recommended_plan_template_code VARCHAR(60);

CREATE INDEX idx_risk_alert_health_policy
    ON emotion_schema.psychological_alert_logs (user_id, benchmark_policy_key, created_at DESC)
    WHERE deleted_at IS NULL AND alert_category = 'HEALTH_BENCHMARK';
