ALTER TABLE emotion_schema.emotion_journals
    ADD COLUMN energy_level SMALLINT,
    ADD COLUMN stress_level SMALLINT,
    ADD COLUMN sleep_quality SMALLINT,
    ADD CONSTRAINT ck_emotion_journal_energy_level CHECK (energy_level BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_emotion_journal_stress_level CHECK (stress_level BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_emotion_journal_sleep_quality CHECK (sleep_quality BETWEEN 1 AND 5);
