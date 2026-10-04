-- Preserve seeded timestamps; future policy migrations must supply audit times explicitly.
ALTER TABLE emotion_schema.scoring_policies ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE emotion_schema.benchmark_policies ALTER COLUMN created_at DROP DEFAULT;
