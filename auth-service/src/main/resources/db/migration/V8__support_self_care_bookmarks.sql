ALTER TABLE auth_schema.bookmarks
    DROP CONSTRAINT IF EXISTS ck_bookmarks_target_type;

ALTER TABLE auth_schema.bookmarks
    ADD CONSTRAINT ck_bookmarks_target_type
        CHECK (target_type IN ('ASSESSMENT', 'SELF_CARE_CONTENT'));
