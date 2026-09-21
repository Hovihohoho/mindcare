CREATE TABLE emotion_schema.scoring_policies (
    policy_key VARCHAR(100) PRIMARY KEY,
    assessment_code VARCHAR(50) NOT NULL,
    policy_version VARCHAR(30) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_scoring_policy_version UNIQUE (assessment_code, policy_version)
);

CREATE TABLE emotion_schema.benchmark_policies (
    policy_key VARCHAR(100) PRIMARY KEY,
    assessment_code VARCHAR(50) NOT NULL,
    policy_version VARCHAR(30) NOT NULL,
    interpretation_type VARCHAR(40) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_benchmark_policy_version UNIQUE (assessment_code, policy_version)
);

CREATE TABLE emotion_schema.benchmark_bands (
    id UUID PRIMARY KEY,
    benchmark_policy_key VARCHAR(100) NOT NULL REFERENCES emotion_schema.benchmark_policies(policy_key),
    level VARCHAR(50) NOT NULL,
    minimum_score INTEGER NOT NULL,
    maximum_score INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT ck_benchmark_band_range CHECK (minimum_score <= maximum_score),
    CONSTRAINT uq_benchmark_band_level UNIQUE (benchmark_policy_key, level)
);

INSERT INTO emotion_schema.scoring_policies(policy_key, assessment_code, policy_version, source_url) VALUES
    ('PHQ9_SCORE', 'PHQ-9', '1.0', 'https://doi.org/10.1046/j.1525-1497.2001.016009606.x'),
    ('GAD7_SCORE', 'GAD-7', '1.0', 'https://doi.org/10.1001/archinte.166.10.1092'),
    ('WHO5_SCORE', 'WHO-5', '1.0', 'https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01'),
    ('PSS10_SCORE', 'PSS-10', '1.0', 'https://www.cmu.edu/dietrich/psychology/stress-immunity-disease-lab/scales/html/pssscoring.html');

INSERT INTO emotion_schema.benchmark_policies(policy_key, assessment_code, policy_version, interpretation_type, source_url) VALUES
    ('PHQ9_KROENKE_2001', 'PHQ-9', '1.0', 'SEVERITY_BANDS', 'https://doi.org/10.1046/j.1525-1497.2001.016009606.x'),
    ('GAD7_SPITZER_2006', 'GAD-7', '1.0', 'SEVERITY_BANDS', 'https://doi.org/10.1001/archinte.166.10.1092'),
    ('WHO5_2024', 'WHO-5', '1.0', 'WELL_BEING_THRESHOLD', 'https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01'),
    ('PSS10_TRACKING', 'PSS-10', '1.0', 'TRACKING_ONLY', 'https://www.cmu.edu/dietrich/psychology/stress-immunity-disease-lab/scales/html/pssscoring.html');

INSERT INTO emotion_schema.benchmark_bands(id, benchmark_policy_key, level, minimum_score, maximum_score, display_order) VALUES
    (md5('PHQ9-MINIMAL')::UUID, 'PHQ9_KROENKE_2001', 'MINIMAL', 0, 4, 1),
    (md5('PHQ9-MILD')::UUID, 'PHQ9_KROENKE_2001', 'MILD', 5, 9, 2),
    (md5('PHQ9-MODERATE')::UUID, 'PHQ9_KROENKE_2001', 'MODERATE', 10, 14, 3),
    (md5('PHQ9-MODERATELY-SEVERE')::UUID, 'PHQ9_KROENKE_2001', 'MODERATELY_SEVERE', 15, 19, 4),
    (md5('PHQ9-SEVERE')::UUID, 'PHQ9_KROENKE_2001', 'SEVERE', 20, 27, 5),
    (md5('GAD7-MINIMAL')::UUID, 'GAD7_SPITZER_2006', 'MINIMAL', 0, 4, 1),
    (md5('GAD7-MILD')::UUID, 'GAD7_SPITZER_2006', 'MILD', 5, 9, 2),
    (md5('GAD7-MODERATE')::UUID, 'GAD7_SPITZER_2006', 'MODERATE', 10, 14, 3),
    (md5('GAD7-SEVERE')::UUID, 'GAD7_SPITZER_2006', 'SEVERE', 15, 21, 4),
    (md5('WHO5-LOW')::UUID, 'WHO5_2024', 'LOW_WELL_BEING', 0, 49, 1),
    (md5('WHO5-ADEQUATE')::UUID, 'WHO5_2024', 'ADEQUATE_WELL_BEING', 50, 100, 2);

ALTER TABLE emotion_schema.assessment_results
    ADD COLUMN normalized_score INTEGER,
    ADD COLUMN interpretation_level VARCHAR(50),
    ADD COLUMN scoring_policy_key VARCHAR(100),
    ADD COLUMN scoring_policy_version VARCHAR(30),
    ADD COLUMN benchmark_policy_key VARCHAR(100),
    ADD COLUMN benchmark_policy_version VARCHAR(30),
    ADD COLUMN risk_signals JSONB;

UPDATE emotion_schema.assessment_results
SET interpretation_level = risk_level
WHERE interpretation_level IS NULL;

UPDATE emotion_schema.assessments
SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP
WHERE code = 'DASS-21' AND status = 'PUBLISHED' AND deleted_at IS NULL;
