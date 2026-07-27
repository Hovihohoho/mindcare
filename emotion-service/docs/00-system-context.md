# MindCare - Bối cảnh hệ thống

## Mục đích

MindCare là nền tảng web và mobile hỗ trợ học sinh, sinh viên và người trẻ chủ động theo dõi sức khỏe tinh thần, thực hiện sàng lọc tâm lý, nhận hỗ trợ ban đầu từ AI và kết nối với chuyên gia. Hệ thống hỗ trợ chăm sóc, không thay thế chẩn đoán, điều trị hoặc dịch vụ khẩn cấp.

Tài liệu này đặt `emotion-service` vào bối cảnh toàn hệ thống. Chi tiết riêng của service nằm trong thư mục `docs/`.

## Tác nhân

- **User**: ghi nhật ký, đồng bộ dữ liệu sức khỏe, làm bài đánh giá, xem xu hướng/kết quả/cảnh báo, chat AI và đặt lịch chuyên gia.
- **Expert**: quản lý hồ sơ/lịch, xử lý booking, xem dữ liệu được phép chia sẻ, tư vấn và ghi chú sau buổi.
- **Admin**: quản trị tài khoản/chuyên gia, bộ đánh giá, kho tri thức AI và báo cáo.
- **Hệ thống bên ngoài**: Health Connect/HealthKit ở thiết bị, FCM, email, object storage, cổng thanh toán và Gemini API.

## Kiến trúc logic

```text
Web / Mobile
     |
Nginx / API Gateway -- xác thực, routing, CORS, rate limit
     |
     +-- Auth Service ----- tài khoản, hồ sơ, role, JWT/OTP
     +-- Emotion Service -- nhật ký, health metrics, assessment, risk
     +-- Booking Service -- chuyên gia, lịch, booking, thanh toán, expert chat
     +-- AI Service ------- RAG, AI chat, guardrail, lịch sử AI
     |
Hạ tầng dùng chung: PostgreSQL, Redis, RabbitMQ, object storage, FCM/email
```

Các service triển khai độc lập và giao tiếp đồng bộ bằng REST khi cần phản hồi ngay; tác vụ thông báo, phân tích nền và tích hợp lỏng dùng message queue. Eureka được mô tả là tùy chọn; local repository hiện tắt Eureka.

## Trách nhiệm và dữ liệu

| Thành phần | Sở hữu chính | Không nên sở hữu |
|---|---|---|
| Auth Service | user, role, trạng thái tài khoản, xác thực, OTP/token | nhật ký, assessment, booking |
| Emotion Service | emotion journal, health metric, assessment catalog/result, risk alert log | hồ sơ tài khoản, lịch hẹn, nội dung AI |
| Booking Service | expert profile/schedule, booking, payment, consultation note, expert chat | assessment và thuật toán rủi ro |
| AI Service | RAG knowledge/index, AI session/message, guardrail AI | nguồn sự thật về assessment/booking |

PostgreSQL là cơ sở dữ liệu nghiệp vụ duy nhất của `emotion-service`, dùng schema riêng `emotion_schema`. Nhật ký cảm xúc, dữ liệu sức khỏe, assessment, kết quả và cảnh báo đều được lưu trong PostgreSQL. Redis (nếu bổ sung) chỉ được dùng làm cache/dữ liệu tạm thời, không phải nguồn sự thật. Xem `docs/10-decisions.md`.

## Luồng nghiệp vụ liên service

### Theo dõi và cảnh báo

1. Mobile xin quyền Health Connect/HealthKit tại thiết bị.
2. Client ghi nhật ký hoặc gửi batch health metrics qua Gateway tới Emotion Service.
3. Emotion Service validate, lưu dữ liệu và có thể lên lịch/phát sự kiện phân tích.
4. Bộ phân tích kết hợp cửa sổ dữ liệu cảm xúc, sức khỏe và assessment gần nhất.
5. Khi vượt ngưỡng, Emotion Service lưu log giải thích và phát yêu cầu thông báo tối thiểu dữ liệu.
6. Notification capability gửi FCM; UI cung cấp hành động an toàn như bài thở, chat AI hoặc đặt lịch.

### Đánh giá tâm lý

1. Admin quản lý bộ câu hỏi và ngưỡng đã được thẩm định.
2. User lấy phiên bản bài đánh giá, nộp câu trả lời.
3. Emotion Service tính điểm theo đúng phiên bản, lưu kết quả và trả phân loại cùng khuyến nghị.
4. Kết quả nguy cơ cao có thể kích hoạt cảnh báo/escalation, nhưng không tự tuyên bố chẩn đoán.

### Chuyên gia xem dữ liệu

Booking Service xác nhận quan hệ tư vấn và phạm vi đồng ý chia sẻ. Emotion Service chỉ trả dữ liệu tối thiểu cho expert đã được ủy quyền; không coi role `EXPERT` đơn thuần là đủ quyền xem mọi user.

## Nguyên tắc an toàn và riêng tư

- Dữ liệu tâm lý và sức khỏe là dữ liệu nhạy cảm: tối thiểu hóa thu thập, mã hóa khi truyền/lưu, kiểm soát truy cập và audit.
- Mọi API ngoài public endpoint đi qua Gateway và vẫn phải kiểm tra quyền ở service.
- Không đưa nội dung nhạy cảm vào log, event hoặc metric quan sát hệ thống.
- Cảnh báo phải đồng cảm, giải thích được và có đường dẫn hỗ trợ con người. Tình huống tự hại/khẩn cấp cần guardrail và quy trình escalation riêng được chuyên gia phê duyệt.
- Chính sách retention, consent, thu hồi consent, export/xóa dữ liệu và nơi lưu trú dữ liệu vẫn là quyết định mở.

## Nguồn tài liệu đầu vào

Baseline này được tổng hợp ngày 2026-07-22 từ ba tài liệu mô tả do nhóm cung cấp và trạng thái repository hiện tại:

- `Đề Tài Đồ Án Tốt Nghiệp.docx`: bài toán, giải pháp, kiến trúc và stack tổng quan.
- `Yeu_cau_chuc_nang.docx`: use case, yêu cầu chi tiết và danh sách entity.
- `YeuCauPhiChucNangMindCare.docx`: bảo mật, hiệu năng, hạ tầng, sơ đồ kiến trúc và phiên bản công nghệ đề xuất.

Các tài liệu Markdown trong repository là bản diễn giải dành cho triển khai. Khi thay đổi yêu cầu gốc, phải cập nhật traceability và decision tương ứng.
