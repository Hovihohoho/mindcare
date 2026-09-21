UPDATE emotion_schema.questions AS question
SET question_text = CASE question.order_index
        WHEN 0 THEN 'Ít hứng thú hoặc không còn thấy vui khi làm mọi việc.'
        WHEN 1 THEN 'Cảm thấy buồn bã, chán nản hoặc tuyệt vọng.'
        WHEN 2 THEN 'Khó ngủ, ngủ không sâu hoặc ngủ quá nhiều.'
        WHEN 3 THEN 'Cảm thấy mệt mỏi hoặc thiếu năng lượng.'
        WHEN 4 THEN 'Ăn kém ngon hoặc ăn quá nhiều.'
        WHEN 5 THEN 'Cảm thấy bản thân tồi tệ, thất bại hoặc làm gia đình thất vọng.'
        WHEN 6 THEN 'Khó tập trung vào công việc, đọc sách hoặc xem truyền hình.'
        WHEN 7 THEN 'Di chuyển hoặc nói chậm, hoặc bồn chồn nhiều hơn bình thường.'
        WHEN 8 THEN 'Có ý nghĩ rằng thà mình không còn sống hoặc muốn làm tổn thương bản thân.'
        ELSE question.question_text
    END,
    updated_at = CURRENT_TIMESTAMP
FROM emotion_schema.assessments AS assessment
WHERE question.assessment_id = assessment.id
  AND assessment.code = 'PHQ-9'
  AND question.deleted_at IS NULL
  AND question.order_index BETWEEN 0 AND 8;

UPDATE emotion_schema.assessments
SET title = 'PHQ-9 - Sàng lọc triệu chứng trầm cảm',
    description = 'Trong hai tuần vừa qua, hãy chọn mức độ thường xuyên bạn gặp từng vấn đề. Kết quả chỉ mang tính sàng lọc, không thay thế chẩn đoán y khoa.',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'PHQ-9'
  AND deleted_at IS NULL;

UPDATE emotion_schema.answer_options AS answer
SET option_text = CASE answer.order_index
        WHEN 0 THEN 'Không hề'
        WHEN 1 THEN 'Vài ngày'
        WHEN 2 THEN 'Hơn một nửa số ngày'
        WHEN 3 THEN 'Gần như mỗi ngày'
        ELSE answer.option_text
    END,
    updated_at = CURRENT_TIMESTAMP
FROM emotion_schema.questions AS question
JOIN emotion_schema.assessments AS assessment ON assessment.id = question.assessment_id
WHERE answer.question_id = question.id
  AND assessment.code = 'PHQ-9'
  AND answer.deleted_at IS NULL
  AND question.deleted_at IS NULL;
