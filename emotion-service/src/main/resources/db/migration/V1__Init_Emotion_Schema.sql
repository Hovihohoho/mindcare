-- Khởi tạo schema nếu chưa tồn tại
CREATE SCHEMA IF NOT EXISTS emotion_schema;
SET search_path TO emotion_schema;

-- Bật extension để Postgres có thể tự động sinh UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Bảng lưu trữ nhật ký cảm xúc
CREATE TABLE emotion_journals (
                                  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                  user_id UUID NOT NULL,
                                  emotion_type VARCHAR(50) NOT NULL,
                                  content TEXT,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  deleted_at TIMESTAMP
);

-- 2. Bảng lưu trữ dữ liệu sinh trắc học đồng bộ
CREATE TABLE health_metrics (
                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                user_id UUID NOT NULL,
                                metric_type VARCHAR(50) NOT NULL, -- SLEEP_HOURS, HEART_RATE, STEP_COUNT
                                metric_value NUMERIC(10, 2) NOT NULL,
                                unit VARCHAR(20),
                                source_type VARCHAR(50) NOT NULL, -- APPLE_HEALTH, GOOGLE_HEALTH, MANUAL
                                recorded_at TIMESTAMP NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                deleted_at TIMESTAMP
);

-- 3. Bảng danh sách các bài Test Tâm lý
CREATE TABLE assessments (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             code VARCHAR(50) UNIQUE NOT NULL, -- DASS-21, PHQ-9, GAD-7
                             title VARCHAR(255) NOT NULL,
                             description TEXT,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             deleted_at TIMESTAMP
);

-- 4. Bảng câu hỏi thuộc bài Test
CREATE TABLE questions (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           assessment_id UUID NOT NULL,
                           question_text TEXT NOT NULL,
                           order_index INT NOT NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           deleted_at TIMESTAMP,
                           CONSTRAINT fk_question_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
);

-- 5. Bảng lựa chọn đáp án
CREATE TABLE answer_options (
                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                question_id UUID NOT NULL,
                                option_text TEXT NOT NULL,
                                score_value INT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                deleted_at TIMESTAMP,
                                CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- 6. Bảng lưu kết quả làm bài của User
CREATE TABLE assessment_results (
                                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    user_id UUID NOT NULL,
                                    assessment_id UUID NOT NULL,
                                    total_score INT NOT NULL,
                                    risk_level VARCHAR(50) NOT NULL, -- NORMAL, MILD, MODERATE, SEVERE, EXTREME
                                    answers_detail JSONB, -- Lưu chi tiết các câu trả lời dạng JSON để truy vết
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    deleted_at TIMESTAMP,
                                    CONSTRAINT fk_result_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id)
);

-- 7. Bảng ghi log cảnh báo tâm lý tự động
CREATE TABLE psychological_alert_logs (
                                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                          user_id UUID NOT NULL,
                                          alert_level VARCHAR(50) NOT NULL,
                                          trigger_reason TEXT NOT NULL,
                                          is_notified BOOLEAN DEFAULT FALSE,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          deleted_at TIMESTAMP
);

-- Đánh index để tối ưu truy vấn dữ liệu lớn
CREATE INDEX idx_emotion_journals_user ON emotion_journals(user_id, created_at);
CREATE INDEX idx_health_metrics_user ON health_metrics(user_id, recorded_at);
CREATE INDEX idx_assessment_results_user ON assessment_results(user_id, created_at);