ALTER TABLE emotion_schema.scoring_policies
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE emotion_schema.benchmark_policies
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
