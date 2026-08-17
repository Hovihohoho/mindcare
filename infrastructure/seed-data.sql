-- Local/demo seed data for the retained MindCare capabilities.
-- All demo accounts use the password: MindCare@123

INSERT INTO auth_schema.users
    (id, role_id, email, password_hash, full_name, is_active, email_verified, created_at)
SELECT md5('mindcare-demo-' || account.email)::UUID,
       roles.id,
       account.email,
       '$2a$10$u1wVvYV.h3d8k3z8/0D0VeQaFJIIQiDaQvdD12ECsm7f7F01G5lqS',
       account.full_name,
       TRUE,
       TRUE,
       CURRENT_TIMESTAMP
FROM (VALUES
    ('user1@mindcare.local', 'Nguyễn Minh Anh', 'ROLE_USER'),
    ('user2@mindcare.local', 'Trần Thu Hà', 'ROLE_USER'),
    ('admin@mindcare.local', 'Quản trị MindCare', 'ROLE_ADMIN')
) AS account(email, full_name, role_name)
JOIN auth_schema.roles ON roles.name = account.role_name
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    role_id = EXCLUDED.role_id,
    is_active = TRUE,
    email_verified = TRUE;
