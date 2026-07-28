-- Local/demo seed data for MindCare.
-- All four accounts use the password: MindCare@123

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- auth_schema: 2 users and 2 experts.
WITH accounts(email, full_name, role_name) AS (
    VALUES
        ('user1@mindcare.local', 'Nguyen Minh An', 'ROLE_USER'),
        ('user2@mindcare.local', 'Tran Gia Han', 'ROLE_USER'),
        ('expert1@mindcare.local', 'TS. Le Thanh Binh', 'ROLE_EXPERT'),
        ('expert2@mindcare.local', 'ThS. Pham Thu Ha', 'ROLE_EXPERT')
)
INSERT INTO auth_schema.users (
    id,
    role_id,
    email,
    password_hash,
    full_name,
    is_active,
    email_verified,
    created_at
)
SELECT
    gen_random_uuid(),
    role.id,
    account.email,
    crypt('MindCare@123', gen_salt('bf', 10)),
    account.full_name,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP
FROM accounts AS account
JOIN auth_schema.roles AS role
    ON role.name = account.role_name
ON CONFLICT (email) DO UPDATE
SET role_id = EXCLUDED.role_id,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    is_active = TRUE,
    email_verified = TRUE;

-- emotion_schema: one complete PHQ-9 assessment for local/demo use.
INSERT INTO emotion_schema.assessments (
    id,
    code,
    title,
    description,
    assessment_version,
    status,
    created_at,
    updated_at,
    deleted_at
)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    'PHQ-9',
    'PHQ-9 - Danh gia trieu chung tram cam',
    'Du lieu demo. Ket qua chi mang tinh sang loc, khong thay the chan doan y khoa.',
    1,
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
)
ON CONFLICT (code, assessment_version) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    deleted_at = NULL;

WITH assessment AS (
    SELECT id
    FROM emotion_schema.assessments
    WHERE code = 'PHQ-9'
      AND assessment_version = 1
),
question_seed(order_index, question_text) AS (
    VALUES
        (0, 'It hung thu hoac vui thich khi lam moi viec'),
        (1, 'Cam thay buon ba, chan nan hoac tuyet vong'),
        (2, 'Kho ngu, ngu khong sau giac hoac ngu qua nhieu'),
        (3, 'Cam thay met moi hoac thieu nang luong'),
        (4, 'Chan an hoac an qua nhieu'),
        (5, 'Cam thay ban than toi te, that bai hoac lam gia dinh that vong'),
        (6, 'Kho tap trung vao cong viec, doc sach hoac xem truyen hinh'),
        (7, 'Di chuyen, noi cham hoac bon chon hon binh thuong'),
        (8, 'Co suy nghi rang chet di se tot hon hoac tu lam dau ban than')
)
INSERT INTO emotion_schema.questions (
    id,
    assessment_id,
    question_text,
    order_index,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    md5('mindcare-phq9-question-' || question_seed.order_index)::UUID,
    assessment.id,
    question_seed.question_text,
    question_seed.order_index,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM assessment
CROSS JOIN question_seed
ON CONFLICT (assessment_id, order_index) WHERE deleted_at IS NULL DO UPDATE
SET question_text = EXCLUDED.question_text,
    updated_at = CURRENT_TIMESTAMP;

WITH assessment AS (
    SELECT id
    FROM emotion_schema.assessments
    WHERE code = 'PHQ-9'
      AND assessment_version = 1
),
active_questions AS (
    SELECT question.id, question.order_index
    FROM emotion_schema.questions AS question
    JOIN assessment ON assessment.id = question.assessment_id
    WHERE question.deleted_at IS NULL
),
option_seed(order_index, option_text, score_value) AS (
    VALUES
        (0, 'Khong he', 0),
        (1, 'Vai ngay', 1),
        (2, 'Hon mot nua so ngay', 2),
        (3, 'Gan nhu moi ngay', 3)
)
INSERT INTO emotion_schema.answer_options (
    id,
    question_id,
    option_text,
    score_value,
    order_index,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    md5(
        'mindcare-phq9-option-'
        || active_questions.order_index
        || '-'
        || option_seed.order_index
    )::UUID,
    active_questions.id,
    option_seed.option_text,
    option_seed.score_value,
    option_seed.order_index,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM active_questions
CROSS JOIN option_seed
ON CONFLICT (question_id, order_index) WHERE deleted_at IS NULL DO UPDATE
SET option_text = EXCLUDED.option_text,
    score_value = EXCLUDED.score_value,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;

-- Verify the seeded rows.
SELECT users.id, roles.name AS role, users.email, users.full_name
FROM auth_schema.users AS users
JOIN auth_schema.roles AS roles ON roles.id = users.role_id
WHERE users.email IN (
    'user1@mindcare.local',
    'user2@mindcare.local',
    'expert1@mindcare.local',
    'expert2@mindcare.local'
)
ORDER BY roles.name, users.email;

SELECT
    assessments.id,
    assessments.code,
    assessments.assessment_version,
    assessments.status,
    COUNT(DISTINCT questions.id) AS question_count,
    COUNT(answer_options.id) AS option_count
FROM emotion_schema.assessments AS assessments
LEFT JOIN emotion_schema.questions AS questions
    ON questions.assessment_id = assessments.id
   AND questions.deleted_at IS NULL
LEFT JOIN emotion_schema.answer_options AS answer_options
    ON answer_options.question_id = questions.id
   AND answer_options.deleted_at IS NULL
WHERE assessments.code = 'PHQ-9'
  AND assessments.assessment_version = 1
GROUP BY
    assessments.id,
    assessments.code,
    assessments.assessment_version,
    assessments.status;
