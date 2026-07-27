# MindCare - Bối cảnh Booking Service

## Mục đích hệ thống

MindCare là nền tảng Web/Mobile hỗ trợ người trẻ theo dõi sức khỏe tinh thần, thực hiện sàng lọc, nhận hỗ trợ ban đầu và kết nối với chuyên gia. Hệ thống hỗ trợ chăm sóc; không thay thế chẩn đoán, điều trị hoặc dịch vụ khẩn cấp.

## Kiến trúc logic

```text
Web / Mobile
     |
Nginx / API Gateway -- JWT, routing, CORS, rate limit
     |
     +-- Auth Service ----- tài khoản, role, identity
     +-- Emotion Service -- journal, health metric, assessment, risk
     +-- Booking Service -- chuyên gia, lịch, booking, payment, expert chat
     +-- AI Service ------- AI chat, RAG, knowledge
     |
PostgreSQL, RabbitMQ, Redis, object storage, FCM/email
```

Booking Service sở hữu luồng kết nối người dùng với chuyên gia từ thời điểm quản lý lịch: lịch làm việc, lịch tư vấn, payment, conversation chuyên gia, đánh giá và ghi chú sau tư vấn. Auth Service sở hữu toàn bộ expert profile/catalog.

## Ranh giới dữ liệu

| Service | Sở hữu | Booking Service chỉ tham chiếu |
|---|---|---|
| Auth Service | Tài khoản, role, trạng thái account, JWT và expert profile/catalog gồm chuyên môn, giấy phép, trạng thái duyệt, giá và rating projection | `userId`, `expertUserId`; dữ liệu profile chỉ được đọc qua contract phát hành |
| Emotion Service | Journal, health metric, assessment result, risk signal | Dữ liệu được chia sẻ sau khi Booking xác nhận quan hệ tư vấn/consent |
| Booking Service | Schedules, bookings, payments, expert chat, reviews, consultation notes | External `userId`/`expertUserId`; không tạo FK sang schema/service khác |
| AI Service | AI session/message, RAG, guardrail | Có thể phát recommendation intent chứa expert filter tối thiểu |

PostgreSQL là nguồn sự thật duy nhất của Booking Service, trong schema `booking_schema`. WebSocket chỉ vận chuyển tin nhắn; lịch sử chat vẫn được persist trong PostgreSQL.

## Actor

- **User**: tìm chuyên gia, đặt/hủy lịch, thanh toán, chat, xem ghi chú, đánh giá.
- **Expert**: quản lý hồ sơ chuyên môn tại Auth, quản lý lịch làm việc và booking tại Booking, chat, ghi chú và hoàn tất tư vấn.
- **Admin**: duyệt hồ sơ chuyên môn tại Auth và xem thống kê booking trong phạm vi được cấp quyền.
- **Payment provider**: gửi callback/IPN đã ký.
- **Internal services**: Auth/Emotion/Notification/AI giao tiếp qua REST hoặc event version hóa.

## Luồng chính

### Đặt lịch

1. Booking xác minh expert đủ điều kiện qua Auth; expert tạo slot không trùng lặp.
2. User chọn slot `AVAILABLE`.
3. Booking Service khóa slot, tạo booking `PAYMENT_PENDING`, snapshot giá và chuyển slot sang `HELD` trong 15 phút.
4. User thanh toán ngay; không có bước expert accept/reject.
5. Callback thành công trong thời gian giữ chuyển booking `CONFIRMED`, slot `BOOKED`; thất bại/hết hạn giải phóng slot.
6. Notification “Bạn có lịch hẹn mới” được yêu cầu qua outbox sau khi confirm, không gửi trực tiếp trong transaction.

### Thanh toán

1. Checkout instant tạo hold và payment attempt bằng `Idempotency-Key`.
2. Service tạo payment `PENDING`, gọi provider ngoài transaction dữ liệu hoặc theo orchestration được thiết kế.
3. Callback được xác minh chữ ký và deduplicate theo provider event ID.
4. Thành công cập nhật payment và booking atomically; thất bại/hết hạn xử lý theo lifecycle được duyệt.

### Tư vấn

1. Booking `CONFIRMED` cho phép conversation từ 24 giờ trước đến 24 giờ sau buổi tư vấn.
2. Tin nhắn được authorize theo participant và persist trước/đồng thời với phát WebSocket.
3. Expert ghi consultation note và hoàn tất booking.
4. User có thể đánh giá đúng một lần cho booking `COMPLETED`.

## Nguồn tài liệu

Baseline ngày 2026-07-25 được tổng hợp từ:

- `Đề Tài Đồ Án Tốt Nghiệp.docx`;
- `Yeu_cau_chuc_nang.docx`;
- `YeuCauPhiChucNangMindCare.docx`;
- skeleton và cấu hình hiện có của repository MindCare.

Khi tài liệu mâu thuẫn, ưu tiên kiến trúc mới hơn xác định PostgreSQL là nguồn lưu trữ nghiệp vụ duy nhất; các khác biệt còn ảnh hưởng contract được ghi trong `10-decisions.md`.
