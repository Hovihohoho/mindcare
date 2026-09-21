DO $$
DECLARE
    user_role_id INTEGER;
    expert_role_id INTEGER;
BEGIN
    SELECT id INTO user_role_id FROM auth_schema.roles WHERE name = 'ROLE_USER';
    SELECT id INTO expert_role_id FROM auth_schema.roles WHERE name = 'ROLE_EXPERT';

    IF expert_role_id IS NOT NULL THEN
        UPDATE auth_schema.users
        SET role_id = user_role_id
        WHERE role_id = expert_role_id;

        DELETE FROM auth_schema.roles WHERE id = expert_role_id;
    END IF;
END $$;

DELETE FROM auth_schema.bookmarks WHERE target_type = 'EXPERT';

ALTER TABLE auth_schema.bookmarks
    DROP CONSTRAINT IF EXISTS ck_bookmarks_target_type;

ALTER TABLE auth_schema.bookmarks
    ADD CONSTRAINT ck_bookmarks_target_type
        CHECK (target_type IN ('ASSESSMENT'));

DROP TABLE IF EXISTS auth_schema.expert_documents;

DROP INDEX IF EXISTS auth_schema.idx_users_expert_status;

ALTER TABLE auth_schema.users
    DROP COLUMN IF EXISTS headline,
    DROP COLUMN IF EXISTS specialties,
    DROP COLUMN IF EXISTS years_of_experience,
    DROP COLUMN IF EXISTS consultation_fee,
    DROP COLUMN IF EXISTS workplace,
    DROP COLUMN IF EXISTS education,
    DROP COLUMN IF EXISTS expert_status,
    DROP COLUMN IF EXISTS expert_review_reason,
    DROP COLUMN IF EXISTS expert_submitted_at,
    DROP COLUMN IF EXISTS expert_reviewed_at;

DROP SCHEMA IF EXISTS booking_schema CASCADE;
