-- Local/demo seed data for MindCare.
-- All demo accounts use the password: MindCare@123

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- auth_schema: users, approved experts, applicants, and one local admin.
WITH accounts(email, full_name, role_name) AS (
    VALUES
        ('admin@mindcare.local', 'Quản trị MindCare', 'ROLE_ADMIN'),
        ('user1@mindcare.local', 'Nguyễn Minh An', 'ROLE_USER'),
        ('user2@mindcare.local', 'Trần Gia Hân', 'ROLE_USER'),
        ('user3@mindcare.local', 'Lê Hoàng Nam', 'ROLE_USER'),
        ('user4@mindcare.local', 'Phạm Ngọc Mai', 'ROLE_USER'),
        ('user5@mindcare.local', 'Vũ Thanh Tùng', 'ROLE_USER'),
        ('user6@mindcare.local', 'Đỗ Khánh Linh', 'ROLE_USER'),
        ('expert1@mindcare.local', 'TS. Lê Thanh Bình', 'ROLE_EXPERT'),
        ('expert2@mindcare.local', 'ThS. Phạm Thu Hà', 'ROLE_EXPERT'),
        ('expert3@mindcare.local', 'BS. Nguyễn Anh Khoa', 'ROLE_EXPERT'),
        ('expert4@mindcare.local', 'ThS. Trần Mỹ Duyên', 'ROLE_EXPERT'),
        ('expert5@mindcare.local', 'CN. Võ Minh Châu', 'ROLE_EXPERT'),
        ('expert6@mindcare.local', 'TS. Đặng Quốc Việt', 'ROLE_EXPERT'),
        ('expert7@mindcare.local', 'ThS. Bùi Thanh Thảo', 'ROLE_EXPERT'),
        ('expert8@mindcare.local', 'BS. Hoàng Gia Bảo', 'ROLE_EXPERT'),
        ('pending.expert@mindcare.local', 'Nguyễn Tú Uyên', 'ROLE_USER'),
        ('rejected.expert@mindcare.local', 'Trần Đức Long', 'ROLE_USER')
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

WITH expert_profiles(email, headline, specialties, experience, fee, workplace, education) AS (
    VALUES
        ('expert1@mindcare.local', 'Chuyên gia tâm lý lâm sàng', 'Trầm cảm, lo âu, stress', 12, 450000, 'MindCare Center', 'Tiến sĩ Tâm lý học lâm sàng'),
        ('expert2@mindcare.local', 'Tham vấn tâm lý gia đình', 'Hôn nhân, gia đình, nuôi dạy con', 9, 380000, 'An Nhiên Clinic', 'Thạc sĩ Tâm lý học'),
        ('expert3@mindcare.local', 'Bác sĩ sức khỏe tâm thần', 'Rối loạn giấc ngủ, lo âu', 15, 550000, 'Bệnh viện Tâm An', 'Bác sĩ chuyên khoa Tâm thần'),
        ('expert4@mindcare.local', 'Chuyên gia tâm lý học đường', 'Học đường, định hướng nghề nghiệp', 7, 320000, 'Trung tâm Đồng Hành', 'Thạc sĩ Tâm lý giáo dục'),
        ('expert5@mindcare.local', 'Chuyên viên tham vấn cá nhân', 'Tự tin, giao tiếp, quản lý cảm xúc', 5, 280000, 'MindCare Online', 'Cử nhân Tâm lý học'),
        ('expert6@mindcare.local', 'Chuyên gia trị liệu nhận thức hành vi', 'CBT, ám ảnh, hoảng sợ', 14, 600000, 'Viện Tâm lý Việt', 'Tiến sĩ Tâm lý trị liệu'),
        ('expert7@mindcare.local', 'Chuyên gia tâm lý trẻ em', 'Trẻ em, vị thành niên, ADHD', 8, 400000, 'Phòng khám Mầm Xanh', 'Thạc sĩ Tâm lý trẻ em'),
        ('expert8@mindcare.local', 'Bác sĩ tư vấn sức khỏe tinh thần', 'Kiệt sức, cân bằng cuộc sống', 11, 500000, 'Bệnh viện Hạnh Phúc', 'Bác sĩ chuyên khoa I')
)
UPDATE auth_schema.users AS users
SET expert_status = 'APPROVED',
    headline = profile.headline,
    specialties = profile.specialties,
    years_of_experience = profile.experience,
    consultation_fee = profile.fee,
    workplace = profile.workplace,
    education = profile.education,
    bio = 'Chuyên gia đã được MindCare xác minh. Đồng hành tôn trọng, bảo mật và dựa trên bằng chứng.',
    expert_submitted_at = CURRENT_TIMESTAMP - INTERVAL '30 days',
    expert_reviewed_at = CURRENT_TIMESTAMP - INTERVAL '28 days'
FROM expert_profiles AS profile
WHERE users.email = profile.email;

UPDATE auth_schema.users
SET expert_status = 'PENDING',
    headline = 'Chuyên viên tham vấn tâm lý',
    specialties = 'Stress và kỹ năng thích nghi',
    years_of_experience = 3,
    consultation_fee = 250000,
    workplace = 'Tư vấn độc lập',
    education = 'Cử nhân Tâm lý học',
    expert_review_reason = NULL,
    expert_submitted_at = CURRENT_TIMESTAMP - INTERVAL '2 days',
    expert_reviewed_at = NULL
WHERE email = 'pending.expert@mindcare.local';

UPDATE auth_schema.users
SET expert_status = 'REJECTED',
    headline = 'Tư vấn viên tâm lý',
    specialties = 'Kỹ năng sống',
    years_of_experience = 1,
    consultation_fee = 200000,
    expert_review_reason = 'Vui lòng bổ sung bằng cấp chuyên môn và chứng chỉ hành nghề.',
    expert_submitted_at = CURRENT_TIMESTAMP - INTERVAL '10 days',
    expert_reviewed_at = CURRENT_TIMESTAMP - INTERVAL '8 days'
WHERE email = 'rejected.expert@mindcare.local';

INSERT INTO auth_schema.expert_documents (id, user_id, document_type, title, file_url, created_at)
SELECT md5('demo-document-' || users.email)::UUID, users.id, 'DEGREE',
       'Bằng cấp chuyên môn đã xác minh', 'https://placehold.co/1200x800?text=MindCare+Verified',
       CURRENT_TIMESTAMP - INTERVAL '29 days'
FROM auth_schema.users AS users
WHERE users.email LIKE 'expert%@mindcare.local'
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, file_url = EXCLUDED.file_url;

INSERT INTO auth_schema.notifications
    (id, user_id, title, message, type, action_url, read_at, created_at)
SELECT md5('demo-welcome-' || users.email)::UUID, users.id,
       'Chào mừng đến với MindCare',
       'Hồ sơ demo đã sẵn sàng để bạn trải nghiệm các chức năng.',
       'SYSTEM', '/', NULL, CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM auth_schema.users AS users
WHERE users.email LIKE '%@mindcare.local'
ON CONFLICT (id) DO UPDATE SET message = EXCLUDED.message;

INSERT INTO auth_schema.bookmarks (id, user_id, target_type, target_id, created_at)
SELECT md5('demo-bookmark-' || users.email || '-' || experts.email)::UUID,
       users.id, 'EXPERT', experts.id::TEXT, CURRENT_TIMESTAMP - INTERVAL '6 hours'
FROM auth_schema.users AS users
CROSS JOIN auth_schema.users AS experts
WHERE users.email IN ('user1@mindcare.local', 'user2@mindcare.local')
  AND experts.email IN ('expert1@mindcare.local', 'expert2@mindcare.local')
ON CONFLICT (id) DO NOTHING;

-- booking_schema: future availability plus representative booking/payment states.
WITH experts AS (
    SELECT id, email FROM auth_schema.users
    WHERE email LIKE 'expert%@mindcare.local' AND expert_status = 'APPROVED'
), slots AS (
    SELECT experts.id AS expert_id, experts.email, slot_index,
           (CURRENT_DATE + (slot_index / 2 + 1) * INTERVAL '1 day'
             + CASE WHEN slot_index % 2 = 0 THEN INTERVAL '9 hours' ELSE INTERVAL '14 hours' END) AS start_at
    FROM experts CROSS JOIN generate_series(0, 5) AS slot_index
)
INSERT INTO booking_schema.expert_schedules
    (id, expert_user_id, start_at, end_at, status, hold_expires_at, version, created_at, updated_at)
SELECT md5('demo-available-' || email || '-' || slot_index)::UUID,
       expert_id, start_at, start_at + INTERVAL '1 hour', 'AVAILABLE', NULL, 0,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM slots
ON CONFLICT (id) DO UPDATE
SET start_at = EXCLUDED.start_at, end_at = EXCLUDED.end_at, status = 'AVAILABLE',
    hold_expires_at = NULL, updated_at = CURRENT_TIMESTAMP, deleted_at = NULL;

WITH booking_seed(code, user_email, expert_email, days_ago, status, payment_status, price) AS (
    VALUES
        ('completed-1', 'user1@mindcare.local', 'expert1@mindcare.local', 12, 'COMPLETED', 'PAID', 450000),
        ('completed-2', 'user2@mindcare.local', 'expert2@mindcare.local', 8, 'COMPLETED', 'PAID', 380000),
        ('confirmed-1', 'user1@mindcare.local', 'expert3@mindcare.local', -2, 'CONFIRMED', 'PAID', 550000),
        ('confirmed-2', 'user3@mindcare.local', 'expert1@mindcare.local', -4, 'CONFIRMED', 'PAID', 450000),
        ('canceled-1', 'user4@mindcare.local', 'expert4@mindcare.local', 4, 'CANCELED', 'REFUNDED', 320000),
        ('failed-1', 'user5@mindcare.local', 'expert5@mindcare.local', 2, 'PAYMENT_FAILED', 'UNPAID', 280000)
), resolved AS (
    SELECT seed.*, users.id AS user_id, experts.id AS expert_id,
           md5('demo-history-schedule-' || seed.code)::UUID AS schedule_id,
           md5('demo-booking-' || seed.code)::UUID AS booking_id,
           (CURRENT_DATE - seed.days_ago * INTERVAL '1 day' + INTERVAL '10 hours') AS start_at
    FROM booking_seed seed
    JOIN auth_schema.users users ON users.email = seed.user_email
    JOIN auth_schema.users experts ON experts.email = seed.expert_email
)
INSERT INTO booking_schema.expert_schedules
    (id, expert_user_id, start_at, end_at, status, hold_expires_at, version, created_at, updated_at)
SELECT schedule_id, expert_id, start_at, start_at + INTERVAL '1 hour',
       CASE WHEN status IN ('CONFIRMED', 'COMPLETED') THEN 'BOOKED' ELSE 'CANCELLED' END,
       NULL, 0, start_at - INTERVAL '7 days', CURRENT_TIMESTAMP
FROM resolved
ON CONFLICT (id) DO UPDATE SET start_at = EXCLUDED.start_at, end_at = EXCLUDED.end_at,
    status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

WITH booking_seed(code, user_email, expert_email, days_ago, status, payment_status, price) AS (
    VALUES
        ('completed-1', 'user1@mindcare.local', 'expert1@mindcare.local', 12, 'COMPLETED', 'PAID', 450000),
        ('completed-2', 'user2@mindcare.local', 'expert2@mindcare.local', 8, 'COMPLETED', 'PAID', 380000),
        ('confirmed-1', 'user1@mindcare.local', 'expert3@mindcare.local', -2, 'CONFIRMED', 'PAID', 550000),
        ('confirmed-2', 'user3@mindcare.local', 'expert1@mindcare.local', -4, 'CONFIRMED', 'PAID', 450000),
        ('canceled-1', 'user4@mindcare.local', 'expert4@mindcare.local', 4, 'CANCELED', 'REFUNDED', 320000),
        ('failed-1', 'user5@mindcare.local', 'expert5@mindcare.local', 2, 'PAYMENT_FAILED', 'UNPAID', 280000)
), resolved AS (
    SELECT seed.*, users.id AS user_id, experts.id AS expert_id,
           md5('demo-history-schedule-' || seed.code)::UUID AS schedule_id,
           md5('demo-booking-' || seed.code)::UUID AS booking_id,
           (CURRENT_DATE - seed.days_ago * INTERVAL '1 day' + INTERVAL '10 hours') AS start_at
    FROM booking_seed seed
    JOIN auth_schema.users users ON users.email = seed.user_email
    JOIN auth_schema.users experts ON experts.email = seed.expert_email
)
INSERT INTO booking_schema.bookings
    (id, user_id, expert_user_id, schedule_id, idempotency_key, status, note, price, currency,
     payment_status, cancellation_reason, canceled_by, expires_at, confirmed_at, completed_at,
     canceled_at, version, created_at, updated_at)
SELECT booking_id, user_id, expert_id, schedule_id, 'demo-' || code, status,
       'Buổi tư vấn dữ liệu demo', price, 'VND', payment_status,
       CASE WHEN status = 'CANCELED' THEN 'Thay đổi kế hoạch cá nhân' END,
       CASE WHEN status = 'CANCELED' THEN 'USER' END,
       CASE WHEN status = 'PAYMENT_FAILED' THEN start_at - INTERVAL '6 days 23 hours' END,
       CASE WHEN status IN ('COMPLETED', 'CONFIRMED') THEN start_at - INTERVAL '6 days' END,
       CASE WHEN status = 'COMPLETED' THEN start_at + INTERVAL '1 hour' END,
       CASE WHEN status = 'CANCELED' THEN start_at - INTERVAL '2 days' END,
       0, start_at - INTERVAL '7 days', CURRENT_TIMESTAMP
FROM resolved
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, payment_status = EXCLUDED.payment_status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO booking_schema.payments
    (id, booking_id, user_id, amount, currency, payment_method, provider_order_id,
     transaction_code, status, idempotency_key, late_success, created_at, updated_at, paid_at)
SELECT md5('demo-payment-' || bookings.id)::UUID, bookings.id, bookings.user_id,
       bookings.price, 'VND', 'VNPAY', 'DEMO-' || bookings.id,
       CASE WHEN bookings.payment_status IN ('PAID', 'REFUNDED') THEN 'TX-' || bookings.id END,
       CASE WHEN bookings.payment_status = 'REFUNDED' THEN 'REFUNDED'
            WHEN bookings.payment_status = 'PAID' THEN 'SUCCESS' ELSE 'FAILED' END,
       'demo-payment-' || bookings.id, FALSE, bookings.created_at, CURRENT_TIMESTAMP,
       CASE WHEN bookings.payment_status IN ('PAID', 'REFUNDED') THEN bookings.created_at + INTERVAL '5 minutes' END
FROM booking_schema.bookings AS bookings
WHERE bookings.id IN (
    md5('demo-booking-completed-1')::UUID, md5('demo-booking-completed-2')::UUID,
    md5('demo-booking-confirmed-1')::UUID, md5('demo-booking-confirmed-2')::UUID,
    md5('demo-booking-canceled-1')::UUID, md5('demo-booking-failed-1')::UUID)
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

INSERT INTO booking_schema.expert_reviews
    (id, booking_id, user_id, expert_user_id, rating, comment, created_at, updated_at)
SELECT md5('demo-review-' || bookings.id)::UUID, bookings.id, bookings.user_id,
       bookings.expert_user_id,
       CASE WHEN bookings.id = md5('demo-booking-completed-1')::UUID THEN 5 ELSE 4 END,
       CASE WHEN bookings.id = md5('demo-booking-completed-1')::UUID
            THEN 'Chuyên gia lắng nghe và đưa ra hướng dẫn rất thực tế.'
            ELSE 'Buổi tư vấn hữu ích, không gian trao đổi an toàn.' END,
       bookings.completed_at + INTERVAL '2 hours', bookings.completed_at + INTERVAL '2 hours'
FROM booking_schema.bookings AS bookings
WHERE bookings.id IN (md5('demo-booking-completed-1')::UUID, md5('demo-booking-completed-2')::UUID)
ON CONFLICT (id) DO UPDATE SET rating = EXCLUDED.rating, comment = EXCLUDED.comment;

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

INSERT INTO emotion_schema.emotion_journals
    (id, user_id, emotion_type, content, created_at, updated_at)
SELECT md5('demo-journal-' || users.email || '-' || day_index)::UUID,
       users.id,
       (ARRAY['VERY_HAPPY', 'HAPPY', 'NEUTRAL', 'SAD', 'STRESSED'])[(day_index % 5) + 1],
       (ARRAY[
           'Hôm nay mình hoàn thành được mục tiêu nhỏ và cảm thấy tích cực hơn.',
           'Dành thời gian nghỉ ngơi và tập thở giúp mình bình tĩnh.',
           'Có một vài lo lắng về công việc nhưng mình đang học cách sắp xếp lại.',
           'Tâm trạng hơi trùng xuống, mình đã chủ động nói chuyện với người thân.',
           'Cơ thể khá mệt nên tối nay mình sẽ ngủ sớm hơn.'
       ])[(day_index % 5) + 1],
       CURRENT_TIMESTAMP - day_index * INTERVAL '1 day',
       CURRENT_TIMESTAMP - day_index * INTERVAL '1 day'
FROM auth_schema.users AS users
CROSS JOIN generate_series(1, 10) AS day_index
WHERE users.email IN ('user1@mindcare.local', 'user2@mindcare.local', 'user3@mindcare.local')
ON CONFLICT (id) DO UPDATE SET emotion_type = EXCLUDED.emotion_type,
    content = EXCLUDED.content, updated_at = EXCLUDED.updated_at;

INSERT INTO emotion_schema.health_metrics
    (id, user_id, metric_type, metric_value, unit, source_type, recorded_at,
     created_at, updated_at, external_sample_id)
SELECT md5('demo-sleep-' || users.email || '-' || day_index)::UUID,
       users.id, 'SLEEP_HOURS', 6.5 + ((day_index % 4) * 0.4), 'hours', 'MANUAL',
       CURRENT_TIMESTAMP - day_index * INTERVAL '1 day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       'demo-sleep-' || day_index
FROM auth_schema.users AS users
CROSS JOIN generate_series(1, 7) AS day_index
WHERE users.email IN ('user1@mindcare.local', 'user2@mindcare.local')
ON CONFLICT (id) DO UPDATE SET metric_value = EXCLUDED.metric_value,
    recorded_at = EXCLUDED.recorded_at, updated_at = CURRENT_TIMESTAMP;

WITH assessment AS (
    SELECT id FROM emotion_schema.assessments
    WHERE code = 'PHQ-9' AND assessment_version = 1
), result_seed(email, score, risk, days_ago) AS (
    VALUES
        ('user1@mindcare.local', 5, 'MILD', 14),
        ('user1@mindcare.local', 3, 'NORMAL', 2),
        ('user2@mindcare.local', 11, 'MODERATE', 5),
        ('user3@mindcare.local', 17, 'SEVERE', 1)
)
INSERT INTO emotion_schema.assessment_results
    (id, user_id, assessment_id, total_score, risk_level, answers_detail,
     assessment_version, scoring_rule_version, idempotency_key, submission_hash,
     screening_notice, recommendations, created_at, updated_at)
SELECT md5('demo-result-' || seed.email || '-' || seed.days_ago)::UUID,
       users.id, assessment.id, seed.score, seed.risk,
       jsonb_build_object('demo', TRUE, 'score', seed.score), 1, 'phq9-v1',
       'demo-result-' || seed.days_ago, md5(seed.email || seed.score),
       'Kết quả chỉ mang tính sàng lọc và hỗ trợ, không thay thế chẩn đoán y khoa.',
       '["Duy trì lịch sinh hoạt đều đặn", "Trao đổi với chuyên gia khi cần"]'::jsonb,
       CURRENT_TIMESTAMP - seed.days_ago * INTERVAL '1 day', CURRENT_TIMESTAMP
FROM result_seed seed
JOIN auth_schema.users users ON users.email = seed.email
CROSS JOIN assessment
ON CONFLICT (id) DO UPDATE SET total_score = EXCLUDED.total_score,
    risk_level = EXCLUDED.risk_level, updated_at = CURRENT_TIMESTAMP;

DO $$
BEGIN
IF to_regclass('ai_schema.knowledge_documents') IS NOT NULL THEN
INSERT INTO ai_schema.knowledge_documents
    (id, title, content, source_url, document_type, is_active,
     original_filename, mime_type, processing_status, indexed_at, created_at, updated_at)
VALUES
    (md5('demo-ai-phq9')::UUID, 'Hướng dẫn tổng quan về PHQ-9',
     'PHQ-9 là công cụ sàng lọc gồm chín câu hỏi. Kết quả không thay thế chẩn đoán của chuyên gia y tế.',
     'https://www.phqscreeners.com/', 'GUIDELINE', TRUE, NULL, 'text/plain', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (md5('demo-ai-gad7')::UUID, 'Hướng dẫn tổng quan về GAD-7',
     'GAD-7 hỗ trợ sàng lọc mức độ triệu chứng lo âu. Người dùng cần được khuyến khích tìm hỗ trợ chuyên môn khi triệu chứng kéo dài.',
     'https://www.phqscreeners.com/', 'GUIDELINE', TRUE, NULL, 'text/plain', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (md5('demo-ai-breathing')::UUID, 'Bài tập thở chậm',
     'Hít vào nhẹ nhàng, thở ra chậm và dài hơn nhịp hít vào. Dừng bài tập nếu cảm thấy khó chịu hoặc chóng mặt.',
     NULL, 'SELF_HELP', TRUE, NULL, 'text/plain', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (md5('demo-ai-sleep')::UUID, 'Vệ sinh giấc ngủ',
     'Duy trì giờ ngủ ổn định, hạn chế caffeine buổi chiều và giảm ánh sáng màn hình trước khi ngủ.',
     NULL, 'SELF_HELP', TRUE, NULL, 'text/plain', 'READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, content = EXCLUDED.content,
    processing_status = 'READY', updated_at = CURRENT_TIMESTAMP;
END IF;
END $$;

\ir /opt/mindcare/ai-knowledge-seed.sql

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
