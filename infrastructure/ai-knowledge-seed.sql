DO $$
BEGIN
IF to_regclass('ai_schema.knowledge_documents') IS NOT NULL THEN
INSERT INTO ai_schema.knowledge_documents
    (id, title, content, source_url, document_type, is_active,
     mime_type, processing_status, indexed_at, created_at, updated_at)
VALUES
    (md5('demo-ai-safety-crisis')::UUID, 'An toàn khi có nguy cơ tự hại hoặc tự sát',
     $content$Mục đích: hướng dẫn phản hồi ban đầu an toàn, không thay thế dịch vụ khẩn cấp.

Dấu hiệu cần ưu tiên an toàn gồm người dùng nói muốn chết, muốn biến mất, đã có kế hoạch hoặc phương tiện tự hại, vừa thực hiện hành vi tự hại, không thể cam kết giữ an toàn trong thời gian ngắn, hoặc đang ở cùng người có hành vi bạo lực. Không tranh luận, phán xét hay hứa giữ bí mật tuyệt đối. Hãy phản hồi bình tĩnh, xác nhận rằng tình huống nghiêm trọng và khuyến khích tìm hỗ trợ trực tiếp ngay.

Các bước phù hợp: hỏi người dùng hiện có đang ở nguy hiểm tức thời không; khuyến khích gọi dịch vụ khẩn cấp tại địa phương hoặc đến khoa cấp cứu gần nhất; đề nghị họ liên hệ một người đáng tin cậy và không ở một mình; nếu an toàn, di chuyển xa thuốc, vũ khí hoặc phương tiện có thể gây hại. Không cung cấp mô tả phương pháp tự hại. Không biến chatbot thành kênh hỗ trợ duy nhất.

Nếu nguy cơ không tức thời nhưng suy nghĩ tự hại xuất hiện, vẫn khuyến khích đặt lịch với chuyên gia sức khỏe tâm thần sớm và xây dựng kế hoạch an toàn cá nhân. Kế hoạch nên gồm dấu hiệu cảnh báo, cách tự ổn định, người có thể liên hệ, dịch vụ chuyên môn và cách giảm tiếp cận phương tiện nguy hiểm.$content$,
     'https://www.nimh.nih.gov/health/publications/warning-signs-of-suicide', 'SAFETY', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (md5('demo-ai-stress')::UUID, 'Nhận biết và quản lý căng thẳng hằng ngày',
     $content$Căng thẳng là phản ứng tự nhiên trước yêu cầu hoặc tình huống khó khăn. Một mức căng thẳng ngắn hạn có thể giúp tập trung, nhưng căng thẳng kéo dài có thể ảnh hưởng giấc ngủ, khả năng tập trung, cảm xúc, tiêu hóa và sức khỏe thể chất. Mỗi người phản ứng khác nhau; có triệu chứng không đồng nghĩa với một chẩn đoán cụ thể.

Dấu hiệu thường gặp: khó thư giãn, dễ cáu, lo lắng, đau đầu, căng cơ, thay đổi ăn uống, ngủ kém và giảm hiệu quả học tập hoặc công việc. Nên quan sát ba yếu tố: triệu chứng kéo dài bao lâu, mức độ ảnh hưởng sinh hoạt và khả năng tự phục hồi sau nghỉ ngơi.

Các hành động ít rủi ro: duy trì giờ ngủ và bữa ăn tương đối đều; chia công việc thành bước nhỏ; vận động phù hợp thể trạng; giới hạn rượu, thuốc lá và caffeine nếu chúng làm triệu chứng nặng hơn; dành vài phút cho bài tập chú ý hiện tại; kết nối với người đáng tin cậy. Chọn một thay đổi nhỏ có thể thực hiện hôm nay thay vì yêu cầu thay đổi toàn bộ lối sống.

Nên tìm chuyên gia khi căng thẳng kéo dài, gây suy giảm rõ rệt, đi kèm cơn hoảng sợ, sử dụng chất để đối phó hoặc có ý nghĩ tự hại. Tự chăm sóc bổ trợ chứ không thay thế chăm sóc chuyên môn.$content$,
     'https://www.who.int/en/news-room/questions-and-answers/item/stress', 'GUIDELINE', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (md5('demo-ai-depression')::UUID, 'Thông tin nền tảng về triệu chứng trầm cảm',
     $content$Trầm cảm khác với cảm giác buồn thoáng qua. Các biểu hiện có thể gồm khí sắc buồn hoặc trống rỗng, mất hứng thú, giảm năng lượng, thay đổi giấc ngủ hoặc ăn uống, khó tập trung, cảm giác vô dụng hay tội lỗi và suy nghĩ về cái chết. Không phải ai cũng có mọi triệu chứng và mức độ ảnh hưởng khác nhau.

Chatbot không được kết luận người dùng mắc trầm cảm dựa trên một câu trả lời hoặc điểm sàng lọc. PHQ-9 chỉ hỗ trợ nhận diện mức độ triệu chứng và theo dõi thay đổi; chẩn đoán cần chuyên gia đánh giá bối cảnh, thời gian, chức năng, bệnh lý cơ thể, thuốc và các nguyên nhân khác.

Hỗ trợ ban đầu có thể tập trung vào lắng nghe, giảm tự trách, khuyến khích nhịp sinh hoạt cơ bản và chọn hoạt động nhỏ có ý nghĩa. Tránh những câu như “hãy nghĩ tích cực” hoặc khẳng định chắc chắn rằng một kỹ thuật sẽ chữa khỏi. Nếu triệu chứng kéo dài khoảng hai tuần, tái diễn, hoặc ảnh hưởng học tập, công việc và quan hệ, nên trao đổi với bác sĩ hoặc chuyên gia tâm lý.

Mọi đề cập đến tự sát hoặc tự hại phải chuyển sang quy trình ưu tiên an toàn: đánh giá nguy hiểm tức thời, khuyến khích dịch vụ khẩn cấp tại địa phương và tìm người đáng tin cậy ở bên.$content$,
     'https://www.who.int/news-room/fact-sheets/detail/depression', 'CONDITION', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (md5('demo-ai-anxiety')::UUID, 'Thông tin nền tảng về lo âu và cơn hoảng sợ',
     $content$Lo âu đôi khi là phản ứng bình thường. Cần quan tâm hơn khi sợ hãi hoặc lo lắng quá mức, khó kiểm soát, kéo dài và cản trở sinh hoạt. Biểu hiện có thể gồm bồn chồn, căng cơ, khó ngủ, khó tập trung, tim đập nhanh, run, vã mồ hôi hoặc khó chịu ở bụng. Các triệu chứng cơ thể cũng có thể do nguyên nhân y khoa; chatbot không nên tự quy tất cả cho lo âu.

Trong lúc lo âu tăng, có thể hướng dẫn người dùng đặt chân vững trên sàn, quan sát môi trường, thở chậm ở mức dễ chịu và gọi tên điều đang xảy ra mà không phán xét. Không yêu cầu hít quá sâu hoặc nín thở lâu. Với cơn hoảng sợ, nhắc rằng cảm giác rất khó chịu nhưng nên tìm cấp cứu nếu có đau ngực mới xuất hiện, ngất, khó thở nghiêm trọng hoặc không chắc đây có phải tình trạng y khoa cấp tính.

Về lâu dài, ghi lại tác nhân, mức độ và hành vi né tránh có thể giúp trao đổi với chuyên gia. Các phương pháp điều trị hiệu quả tồn tại, gồm can thiệp tâm lý và khi phù hợp là thuốc do bác sĩ chỉ định. Không khuyến nghị tự dùng hoặc tự ngừng thuốc.$content$,
     'https://www.who.int/news-room/fact-sheets/detail/anxiety-disorders', 'CONDITION', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (md5('demo-ai-sleep')::UUID, 'Giấc ngủ và sức khỏe tinh thần',
     $content$Giấc ngủ và sức khỏe tinh thần ảnh hưởng hai chiều. Stress, lo âu hoặc khí sắc thấp có thể làm khó ngủ; thiếu ngủ lại làm tăng mệt mỏi, khó tập trung và phản ứng cảm xúc. Một vài đêm ngủ kém chưa đủ để kết luận rối loạn giấc ngủ.

Các thói quen hỗ trợ: duy trì giờ thức dậy tương đối ổn định; tạo khoảng chuyển tiếp yên tĩnh trước ngủ; để phòng ngủ tối, yên và thoải mái; hạn chế caffeine vào cuối ngày; tránh dùng rượu như thuốc ngủ; vận động ban ngày phù hợp; giảm màn hình nếu làm bản thân tỉnh táo. Nếu nằm lâu mà không ngủ, có thể rời giường làm hoạt động nhẹ trong ánh sáng dịu rồi quay lại khi buồn ngủ.

Không đưa ra một số giờ ngủ cứng nhắc cho mọi người và không khuyên dùng thuốc hoặc thực phẩm bổ sung. Nên tìm bác sĩ khi mất ngủ kéo dài, ngáy kèm ngưng thở, buồn ngủ ban ngày gây nguy hiểm, ác mộng nghiêm trọng, hoặc thay đổi giấc ngủ đi cùng khí sắc hưng phấn bất thường. Nếu người dùng lái xe hoặc vận hành máy móc khi quá buồn ngủ, ưu tiên dừng hoạt động nguy hiểm.$content$,
     'https://www.who.int/en/news-room/questions-and-answers/item/stress', 'SELF_HELP', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (md5('demo-ai-grounding')::UUID, 'Bài tập ổn định chú ý và thở chậm',
     $content$Mục tiêu của bài tập ổn định không phải xóa cảm xúc ngay lập tức mà giúp người dùng quay lại hiện tại và có thêm lựa chọn phản ứng.

Bài tập quan sát: đặt hai bàn chân trên sàn hoặc cảm nhận điểm cơ thể đang tiếp xúc với ghế. Nhìn quanh và gọi tên năm điều nhìn thấy, bốn điều có thể chạm, ba âm thanh, hai mùi và một vị. Có thể rút ngắn nếu người dùng thấy quá tải. Mời họ mô tả bằng từ trung tính như màu sắc, hình dạng và nhiệt độ.

Bài tập thở: thả lỏng vai, hít vào nhẹ nhàng bằng nhịp tự nhiên và thở ra chậm hơn một chút. Lặp lại trong một đến hai phút, không ép hít thật sâu và không nín thở. Dừng nếu chóng mặt, đau hoặc khó chịu tăng. Người có bệnh hô hấp hoặc tim mạch nên ưu tiên hướng dẫn của chuyên gia y tế.

Sau bài tập, hỏi mức căng thẳng thay đổi thế nào theo thang 0–10 và chọn bước nhỏ tiếp theo: uống nước, đi bộ ngắn, nhắn người tin cậy hoặc rời khỏi tác nhân nếu an toàn. Bài tập này là kỹ năng đối phó tạm thời, không thay thế điều trị.$content$,
     'https://www.who.int/news-room/feature-stories/mental-well-being-resources-for-the-public/', 'SELF_HELP', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (md5('demo-ai-screening')::UUID, 'Cách diễn giải PHQ-9, GAD-7 và WHO-5 an toàn',
     $content$Các bài PHQ-9, GAD-7 và WHO-5 là công cụ sàng lọc hoặc theo dõi, không phải chẩn đoán độc lập. Kết quả cần được đọc cùng thời gian xuất hiện triệu chứng, ảnh hưởng chức năng, hoàn cảnh sống và đánh giá chuyên môn.

PHQ-9 phản ánh tần suất chín nhóm triệu chứng trầm cảm trong hai tuần gần đây. GAD-7 phản ánh bảy nhóm triệu chứng lo âu trong hai tuần. Điểm cao hơn ở hai thang này thường biểu thị gánh nặng triệu chứng lớn hơn. Riêng câu hỏi tự hại của PHQ-9 phải được xem xét về an toàn bất kể tổng điểm.

WHO-5 đo trạng thái sức khỏe tinh thần tích cực; điểm cao hơn biểu thị trạng thái tốt hơn. Vì chiều điểm ngược với PHQ-9/GAD-7, không được so sánh trực tiếp tổng điểm giữa các thang. Điểm thấp nên dẫn đến trao đổi thêm, không phải kết luận bệnh.

Khi trả kết quả, dùng ngôn ngữ xác suất và hỗ trợ: “kết quả gợi ý”, “có thể cân nhắc trao đổi với chuyên gia”. Không dùng “bạn chắc chắn mắc…”. Khuyến khích theo dõi xu hướng qua thời gian nhưng tránh biến thay đổi nhỏ thành kết luận lâm sàng. Nếu triệu chứng nặng, suy giảm rõ hoặc có nguy cơ tự hại, ưu tiên hỗ trợ chuyên môn.$content$,
     'https://www.phqscreeners.com/', 'SCREENING', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (md5('demo-ai-professional-help')::UUID, 'Khi nào và cách tìm hỗ trợ chuyên môn',
     $content$Nên cân nhắc hỗ trợ chuyên môn khi cảm xúc hoặc triệu chứng kéo dài, tăng dần, tái diễn, ảnh hưởng giấc ngủ, học tập, công việc, chăm sóc bản thân hoặc các mối quan hệ. Cũng nên tìm hỗ trợ sớm khi có cơn hoảng sợ, sử dụng rượu hoặc chất để đối phó, trải nghiệm sang chấn, triệu chứng cơ thể chưa rõ nguyên nhân, hoặc người thân bày tỏ lo ngại.

Chuyên gia phù hợp có thể là bác sĩ, bác sĩ tâm thần, nhà tâm lý lâm sàng hoặc chuyên viên tham vấn có đào tạo, tùy nhu cầu và hệ thống địa phương. Người dùng có thể chuẩn bị: triệu chứng chính, thời điểm bắt đầu, yếu tố làm tốt hơn hoặc nặng hơn, thuốc đang dùng, bệnh lý cơ thể, kết quả sàng lọc và mục tiêu mong muốn. Có quyền hỏi về bằng cấp, cách bảo mật, phương pháp làm việc, chi phí và kế hoạch theo dõi.

Nếu lần gặp đầu chưa phù hợp, có thể trao đổi thẳng hoặc tìm chuyên gia khác; điều này không có nghĩa việc tìm hỗ trợ thất bại. Tự chăm sóc và hỗ trợ từ người thân có giá trị nhưng không thay thế chăm sóc chuyên môn khi mức độ nghiêm trọng cao. Nguy hiểm tức thời, tự hại, bạo lực, mất ý thức hoặc triệu chứng y khoa cấp phải được chuyển đến dịch vụ khẩn cấp tại địa phương.$content$,
     'https://www.who.int/news-room/fact-sheets/detail/mental-health-strengthening-our-response', 'CARE_NAVIGATION', TRUE,
     'text/plain', 'PROCESSING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    content = EXCLUDED.content,
    source_url = EXCLUDED.source_url,
    document_type = EXCLUDED.document_type,
    is_active = TRUE,
    processing_status = CASE
        WHEN ai_schema.knowledge_documents.embedding IS NULL THEN 'PROCESSING'
        ELSE 'READY'
    END,
    processing_error = NULL,
    indexed_at = CASE
        WHEN ai_schema.knowledge_documents.embedding IS NULL THEN NULL
        ELSE ai_schema.knowledge_documents.indexed_at
    END,
    updated_at = CURRENT_TIMESTAMP;

-- Detailed documents above supersede the original one-paragraph snippets.
-- Keep their rows for repeatable seed compatibility, but exclude them from
-- retrieval so they cannot dilute the context supplied to the model.
UPDATE ai_schema.knowledge_documents
SET is_active = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    md5('demo-ai-phq9')::UUID,
    md5('demo-ai-gad7')::UUID,
    md5('demo-ai-breathing')::UUID
);
END IF;
END $$;
