CREATE UNIQUE INDEX uq_assessment_one_draft_version
    ON emotion_schema.assessments (code)
    WHERE status = 'DRAFT' AND deleted_at IS NULL;
