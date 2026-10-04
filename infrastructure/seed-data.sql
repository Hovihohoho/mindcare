-- Local/demo seed data for the retained MindCare capabilities.
-- All demo accounts use the password: MindCare@123

INSERT INTO auth_schema.users
    (id, role_id, email, password_hash, full_name, is_active, email_verified, created_at)
SELECT md5('mindcare-demo-' || account.email)::UUID,
       roles.id,
       account.email,
       '$2a$10$fO4n5EKBMGYJpd9BnQB1x.5Xzo8AoIctzJf8Yj6vV/pIgW8Q9f3aS',
       account.full_name,
       TRUE,
       TRUE,
       CURRENT_TIMESTAMP
FROM (VALUES
    ('user1@mindcare.local', 'Nguyễn Minh Anh', 'ROLE_USER'),
    ('user2@mindcare.local', 'Trần Thu Hà', 'ROLE_USER'),
    ('user3@mindcare.local', 'Lê Hoàng Nam', 'ROLE_USER'),
    ('user4@mindcare.local', 'Phạm Ngọc Mai', 'ROLE_USER'),
    ('user5@mindcare.local', 'Võ Đức Anh', 'ROLE_USER'),
    ('user6@mindcare.local', 'Đặng Khánh Linh', 'ROLE_USER'),
    ('admin@mindcare.local', 'Quản trị MindCare', 'ROLE_ADMIN')
) AS account(email, full_name, role_name)
JOIN auth_schema.roles ON roles.name = account.role_name
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    password_hash = EXCLUDED.password_hash,
    role_id = EXCLUDED.role_id,
    is_active = TRUE,
    email_verified = TRUE;

-- Demo AI conversations: two conversations and four messages per user.
WITH demo_users AS (
    SELECT id, email
    FROM auth_schema.users
    WHERE email IN (
        'user1@mindcare.local', 'user2@mindcare.local', 'user3@mindcare.local',
        'user4@mindcare.local', 'user5@mindcare.local', 'user6@mindcare.local'
    )
), topics(topic_key, title, day_offset) AS (
    VALUES
        ('sleep', 'Cải thiện giấc ngủ', 2),
        ('stress', 'Giảm căng thẳng trong ngày', 1)
)
INSERT INTO ai_schema.ai_conversations (id, user_id, title, created_at, updated_at)
SELECT md5('demo-ai-conversation-' || demo_users.email || '-' || topics.topic_key)::UUID,
       demo_users.id,
       topics.title,
       CURRENT_TIMESTAMP - make_interval(days => topics.day_offset),
       CURRENT_TIMESTAMP - make_interval(days => topics.day_offset) + INTERVAL '15 minutes'
FROM demo_users
CROSS JOIN topics
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    title = EXCLUDED.title,
    updated_at = EXCLUDED.updated_at;

WITH demo_users AS (
    SELECT id, email
    FROM auth_schema.users
    WHERE email IN (
        'user1@mindcare.local', 'user2@mindcare.local', 'user3@mindcare.local',
        'user4@mindcare.local', 'user5@mindcare.local', 'user6@mindcare.local'
    )
), messages(topic_key, message_order, role, content, safety_level, day_offset) AS (
    VALUES
        ('sleep', 1, 'user',
         'Mấy hôm nay mình khó ngủ và thường tỉnh giữa đêm. Mình có thể làm gì tối nay?', 'NONE', 2),
        ('sleep', 2, 'assistant',
         'Bạn có thể thử giảm ánh sáng, tránh màn hình trước giờ ngủ và dành vài phút thở chậm. Nếu tình trạng kéo dài, hãy cân nhắc trao đổi với chuyên gia y tế.', 'NONE', 2),
        ('sleep', 3, 'user',
         'Bạn hướng dẫn mình một bài thở ngắn được không?', 'NONE', 2),
        ('sleep', 4, 'assistant',
         'Hãy hít vào nhẹ nhàng trong 4 giây, giữ 2 giây rồi thở ra trong 6 giây. Lặp lại 5 lần và dừng nếu bạn thấy khó chịu.', 'NONE', 2),
        ('stress', 1, 'user',
         'Hôm nay công việc dồn nhiều nên mình thấy khá căng thẳng.', 'NONE', 1),
        ('stress', 2, 'assistant',
         'Nghe như bạn đang phải xử lý nhiều việc cùng lúc. Mình có thể giúp bạn chia công việc thành vài bước nhỏ và chọn việc cần làm trước.', 'NONE', 1),
        ('stress', 3, 'user',
         'Mình muốn có một kế hoạch nghỉ ngắn trong ngày.', 'NONE', 1),
        ('stress', 4, 'assistant',
         'Bạn có thể thử chu kỳ 25 phút tập trung và 5 phút nghỉ, uống nước, đứng dậy giãn vai và kiểm tra lại mức căng thẳng sau mỗi hai chu kỳ.', 'NONE', 1)
)
INSERT INTO ai_schema.ai_conversation_messages
    (id, conversation_id, role, content, sources_json, safety_level, created_at)
SELECT md5('demo-ai-message-' || demo_users.email || '-' || messages.topic_key || '-' || messages.message_order)::UUID,
       md5('demo-ai-conversation-' || demo_users.email || '-' || messages.topic_key)::UUID,
       messages.role,
       messages.content,
       CASE WHEN messages.role = 'assistant' THEN '[]' ELSE NULL END,
       messages.safety_level,
       CURRENT_TIMESTAMP - make_interval(days => messages.day_offset)
           + make_interval(mins => messages.message_order * 5)
FROM demo_users
CROSS JOIN messages
ON CONFLICT (id) DO UPDATE SET
    content = EXCLUDED.content,
    sources_json = EXCLUDED.sources_json,
    safety_level = EXCLUDED.safety_level,
    created_at = EXCLUDED.created_at;

-- Seven days of emotion journals for each demo user.
WITH demo_users AS (
    SELECT id, email
    FROM auth_schema.users
    WHERE email IN (
        'user1@mindcare.local', 'user2@mindcare.local', 'user3@mindcare.local',
        'user4@mindcare.local', 'user5@mindcare.local', 'user6@mindcare.local'
    )
), journal_days(day_offset, emotion_type, content, energy_level, stress_level, sleep_quality) AS (
    VALUES
        (6, 'NEUTRAL', 'Hôm nay mình dành thời gian đi bộ và cảm thấy đầu óc nhẹ hơn.', 4, 2, 4),
        (5, 'SAD',     'Công việc hơi nhiều nên mình thiếu năng lượng vào cuối ngày.', 2, 4, 3),
        (4, 'HAPPY',   'Mình hoàn thành một việc đã trì hoãn và cảm thấy khá vui.', 4, 2, 4),
        (3, 'STRESSED','Có một cuộc họp quan trọng khiến mình hơi lo lắng.', 3, 4, 3),
        (2, 'NEUTRAL', 'Mình đã nghỉ ngắn và tập thở, mức căng thẳng giảm xuống.', 3, 2, 4),
        (1, 'SAD',     'Hôm nay tâm trạng hơi thấp nhưng mình vẫn cố gắng chăm sóc bản thân.', 2, 3, 3),
        (0, 'HAPPY',   'Mình ngủ tốt hơn và thấy sẵn sàng cho ngày mới.', 4, 2, 5)
)
INSERT INTO emotion_schema.emotion_journals
    (id, user_id, emotion_type, content, energy_level, stress_level, sleep_quality,
     created_at, updated_at, deleted_at)
SELECT md5('demo-emotion-journal-' || demo_users.email || '-' || journal_days.day_offset)::UUID,
       demo_users.id,
       journal_days.emotion_type,
       journal_days.content,
       journal_days.energy_level,
       journal_days.stress_level,
       journal_days.sleep_quality,
       CURRENT_DATE - journal_days.day_offset + TIME '20:00',
       CURRENT_DATE - journal_days.day_offset + TIME '20:00',
       NULL
FROM demo_users
CROSS JOIN journal_days
ON CONFLICT (id) DO UPDATE SET
    emotion_type = EXCLUDED.emotion_type,
    content = EXCLUDED.content,
    energy_level = EXCLUDED.energy_level,
    stress_level = EXCLUDED.stress_level,
    sleep_quality = EXCLUDED.sleep_quality,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at,
    deleted_at = NULL;

-- A current, coherent health story for the primary demo account (user1).
WITH demo_user AS (
    SELECT id, email FROM auth_schema.users WHERE email = 'user1@mindcare.local'
), health_days(day_offset, sleep_hours, steps, resting_hr, spo2, exercise_minutes) AS (
    VALUES
        (6, 6.0,  3200, 78, 97, 15),
        (5, 6.2,  3800, 77, 97, 20),
        (4, 6.5,  4400, 76, 98, 20),
        (3, 6.8,  5100, 75, 98, 25),
        (2, 7.1,  5900, 73, 98, 30),
        (1, 7.3,  6600, 72, 98, 35),
        (0, 7.5,  7200, 71, 98, 40)
)
INSERT INTO emotion_schema.health_metrics
    (id, user_id, metric_type, metric_value, unit, source_type, recorded_at,
     created_at, updated_at, deleted_at, external_sample_id, start_time, end_time,
     source_name, data_origin, source_last_modified_at, record_details)
SELECT md5('demo-health-sleep-' || demo_user.email || '-' || health_days.day_offset)::UUID,
       demo_user.id, 'SLEEP_SESSION', NULL, NULL, 'HEALTH_CONNECT',
       CURRENT_DATE - health_days.day_offset + TIME '06:30',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'demo-sleep-' || health_days.day_offset,
       CURRENT_DATE - health_days.day_offset + TIME '06:30'
           - make_interval(mins => (health_days.sleep_hours * 60)::INTEGER),
       CURRENT_DATE - health_days.day_offset + TIME '06:30',
       'Samsung Health', 'com.sec.android.app.shealth', CURRENT_TIMESTAMP,
       jsonb_build_object('stage', 'TOTAL')
FROM demo_user CROSS JOIN health_days
ON CONFLICT (id) DO UPDATE SET
    recorded_at=EXCLUDED.recorded_at, start_time=EXCLUDED.start_time, end_time=EXCLUDED.end_time,
    source_last_modified_at=EXCLUDED.source_last_modified_at, deleted_at=NULL;

WITH demo_user AS (
    SELECT id, email FROM auth_schema.users WHERE email = 'user1@mindcare.local'
), health_days(day_offset, sleep_hours, steps, resting_hr, spo2, exercise_minutes) AS (
    VALUES (6,6.0,3200,78,97,15),(5,6.2,3800,77,97,20),(4,6.5,4400,76,98,20),
           (3,6.8,5100,75,98,25),(2,7.1,5900,73,98,30),(1,7.3,6600,72,98,35),(0,7.5,7200,71,98,40)
), point_metrics(metric_type, unit) AS (
    VALUES ('STEP_COUNT','count'),('HEART_RATE','bpm'),('SPO2','%')
)
INSERT INTO emotion_schema.health_metrics
    (id, user_id, metric_type, metric_value, unit, source_type, recorded_at,
     created_at, updated_at, deleted_at, external_sample_id, source_name,
     data_origin, source_last_modified_at, record_details)
SELECT md5('demo-health-' || lower(point_metrics.metric_type) || '-' || demo_user.email || '-' || health_days.day_offset)::UUID,
       demo_user.id, point_metrics.metric_type,
       CASE point_metrics.metric_type WHEN 'STEP_COUNT' THEN health_days.steps
            WHEN 'HEART_RATE' THEN health_days.resting_hr ELSE health_days.spo2 END,
       point_metrics.unit, 'HEALTH_CONNECT',
       CURRENT_DATE - health_days.day_offset + CASE point_metrics.metric_type
           WHEN 'STEP_COUNT' THEN TIME '21:00' WHEN 'HEART_RATE' THEN TIME '07:00' ELSE TIME '07:05' END,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'demo-' || lower(point_metrics.metric_type) || '-' || health_days.day_offset,
       'Samsung Health', 'com.sec.android.app.shealth', CURRENT_TIMESTAMP,
       CASE WHEN point_metrics.metric_type='HEART_RATE'
            THEN '{"measurementContext":"RESTING"}'::jsonb ELSE '{}'::jsonb END
FROM demo_user CROSS JOIN health_days CROSS JOIN point_metrics
ON CONFLICT (id) DO UPDATE SET
    metric_value=EXCLUDED.metric_value, recorded_at=EXCLUDED.recorded_at,
    source_last_modified_at=EXCLUDED.source_last_modified_at,
    record_details=EXCLUDED.record_details, deleted_at=NULL;

WITH demo_user AS (
    SELECT id, email FROM auth_schema.users WHERE email = 'user1@mindcare.local'
), exercise_days(day_offset, minutes) AS (
    VALUES (6,15),(5,20),(4,20),(3,25),(2,30),(1,35),(0,40)
)
INSERT INTO emotion_schema.health_metrics
    (id, user_id, metric_type, metric_value, unit, source_type, recorded_at,
     created_at, updated_at, deleted_at, external_sample_id, start_time, end_time,
     source_name, data_origin, source_last_modified_at, record_details)
SELECT md5('demo-health-exercise-' || demo_user.email || '-' || exercise_days.day_offset)::UUID,
       demo_user.id, 'EXERCISE_SESSION', NULL, NULL, 'HEALTH_CONNECT',
       CURRENT_DATE - exercise_days.day_offset + TIME '18:00',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL,
       'demo-exercise-' || exercise_days.day_offset,
       CURRENT_DATE - exercise_days.day_offset + TIME '18:00' - make_interval(mins => exercise_days.minutes),
       CURRENT_DATE - exercise_days.day_offset + TIME '18:00',
       'Samsung Health', 'com.sec.android.app.shealth', CURRENT_TIMESTAMP,
       '{"exerciseType":"WALKING"}'::jsonb
FROM demo_user CROSS JOIN exercise_days
ON CONFLICT (id) DO UPDATE SET
    recorded_at=EXCLUDED.recorded_at, start_time=EXCLUDED.start_time, end_time=EXCLUDED.end_time,
    source_last_modified_at=EXCLUDED.source_last_modified_at, deleted_at=NULL;

-- A recent assessment result that appears in the primary demo user's timeline.
INSERT INTO emotion_schema.assessment_results
    (id, user_id, assessment_id, total_score, risk_level, answers_detail,
     created_at, updated_at, deleted_at, assessment_version, scoring_rule_version,
     idempotency_key, submission_hash, screening_notice, recommendations,
     normalized_score, interpretation_level, scoring_policy_key, scoring_policy_version,
     benchmark_policy_key, benchmark_policy_version, risk_signals)
SELECT md5('demo-recent-phq9-user1')::UUID,
       demo_user.id, assessment.id, 6, 'MILD', '[]'::jsonb,
       CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days', NULL,
       assessment.assessment_version, 'PHQ9_SCORE-1.0', 'demo-user1-recent-phq9',
       md5('demo-user1-recent-phq9'),
       'Kết quả chỉ mang tính sàng lọc và hỗ trợ, không phải chẩn đoán y khoa.',
       '["Duy trì nhật ký cảm xúc mỗi ngày.","Tiếp tục kế hoạch tự chăm sóc và theo dõi giấc ngủ."]'::jsonb,
       NULL, 'MILD', 'PHQ9_SCORE', '1.0', 'PHQ9_KROENKE_2001', '1.0', '[]'::jsonb
FROM auth_schema.users demo_user
JOIN emotion_schema.assessments assessment ON assessment.code='PHQ-9'
    AND assessment.status='PUBLISHED' AND assessment.deleted_at IS NULL
WHERE demo_user.email='user1@mindcare.local'
ON CONFLICT (id) DO UPDATE SET
    created_at=EXCLUDED.created_at, updated_at=EXCLUDED.updated_at,
    recommendations=EXCLUDED.recommendations, deleted_at=NULL;

-- Visible progress in the current week's self-care plan.
WITH selected_activities AS (
    SELECT activity.id, activity.display_order
    FROM emotion_schema.self_care_activities activity
    JOIN emotion_schema.self_care_plans plan ON plan.id=activity.plan_id
    JOIN auth_schema.users demo_user ON demo_user.id=plan.user_id
    WHERE demo_user.email='user1@mindcare.local' AND activity.display_order < 3
), completion_days(display_order, day_offset) AS (
    VALUES (0,0),(0,1),(0,2),(1,0),(1,2),(2,1)
)
INSERT INTO emotion_schema.self_care_completions (id, activity_id, completed_on, created_at)
SELECT md5('demo-care-completion-' || selected_activities.id || '-' || completion_days.day_offset)::UUID,
       selected_activities.id,
       LEAST(CURRENT_DATE, date_trunc('week', CURRENT_DATE)::date + completion_days.day_offset),
       CURRENT_TIMESTAMP
FROM selected_activities
JOIN completion_days ON completion_days.display_order=selected_activities.display_order
ON CONFLICT (id) DO UPDATE SET completed_on=EXCLUDED.completed_on;

-- Stable demo notifications that make the notification centre useful immediately.
INSERT INTO auth_schema.notifications
    (id,user_id,source_event_id,type,title,message,action_url,read_at,created_at)
SELECT md5('demo-notification-welcome-user1')::UUID, demo_user.id,
       md5('demo-event-welcome-user1')::UUID, 'SYSTEM', 'Chào mừng trở lại MindCare',
       'Bạn đã duy trì nhật ký cảm xúc trong 7 ngày. Hãy xem lại xu hướng tuần này.',
       '/emotion/history', NULL, CURRENT_TIMESTAMP - INTERVAL '4 hours'
FROM auth_schema.users demo_user WHERE demo_user.email='user1@mindcare.local'
ON CONFLICT (id) DO UPDATE SET message=EXCLUDED.message, action_url=EXCLUDED.action_url;

INSERT INTO auth_schema.notifications
    (id,user_id,source_event_id,type,title,message,action_url,read_at,created_at)
SELECT md5('demo-notification-care-user1')::UUID, demo_user.id,
       md5('demo-event-care-user1')::UUID, 'SELF_CARE', 'Tiến độ tự chăm sóc tuần này',
       'Bạn đã hoàn thành nhiều hoạt động trong kế hoạch. Một bước nhỏ hôm nay cũng rất đáng ghi nhận.',
       '/care-plan', NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours'
FROM auth_schema.users demo_user WHERE demo_user.email='user1@mindcare.local'
ON CONFLICT (id) DO UPDATE SET message=EXCLUDED.message, action_url=EXCLUDED.action_url;
