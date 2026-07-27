# Thiết kế cơ sở dữ liệu Booking Service

## Quy ước

- PostgreSQL, schema `booking_schema`.
- Tên bảng/cột `snake_case`.
- PK UUID sinh tại application bằng `GenerationType.UUID`; SQL không dùng UUID default.
- Thời gian `TIMESTAMPTZ`; Java tương lai dùng `OffsetDateTime`.
- `created_at`/`updated_at` do Spring Data JPA Auditing; không trigger/default database.
- Mọi bảng nghiệp vụ có `deleted_at`; read mặc định loại soft-delete.
- ID từ service khác là external reference, không có FK xuyên schema.
- Enum application lưu `VARCHAR` và có check constraint.
- Monetary value dùng `NUMERIC(12,2)` + currency ISO-4217 dạng `CHAR(3)`.

## Bảng và aggregate

### `expert_schedules`

Slot theo khoảng `[start_at,end_at)`.

- External `expert_user_id`, không FK sang Auth.
- status: `AVAILABLE`, `HELD`, `BOOKED`, `CANCELLED`.
- `hold_expires_at` bắt buộc khi `HELD`, null ở status khác.
- exact active slot unique.
- Index query slot trống theo expert/start.

Database chưa dùng exclusion constraint `tstzrange` để tránh phụ thuộc extension/DB privilege. Service bắt buộc lấy transaction-level advisory lock theo `expert_user_id` trước khi kiểm tra overlap; nếu workload cần writer ngoài service, bổ sung exclusion constraint bằng migration sau.

### `bookings`

Aggregate lifecycle trung tâm.

- External `user_id`.
- External `expert_user_id` và FK nội bộ `schedule_id`.
- Snapshot `price`/`currency`.
- Status booking và payment.
- `idempotency_key` unique theo user.
- Timestamps hold/confirm/cancellation request-decision/complete/cancel và actor hủy.
- Status: `PAYMENT_PENDING`, `CONFIRMED`, `CANCELLATION_PENDING`, `CANCELED`, `COMPLETED`, `USER_NO_SHOW`, `EXPERT_NO_SHOW`, `PAYMENT_FAILED`, `EXPIRED`.
- Partial unique index ngăn hai booking active cùng schedule.
- Index history theo user và expert.

### `conversations`, `messages`

- Một conversation/booking.
- Message tham chiếu conversation và external sender ID.
- Payload check theo message type.
- Keyset index `(conversation_id,created_at,id)`.

### `expert_reviews`

- Một review/booking.
- FK booking, external user ID và external expert user ID.
- Rating `1..5`.

### `consultation_notes`

- Một note/booking.
- Observation, recommendation, recovery plan và visibility.
- Không lưu trường tên `diagnosis` để tránh contract ngầm biến note thành chẩn đoán tự động.

### `payments`

- Nhiều attempt lịch sử/booking, tối đa một `PENDING`.
- Provider order/transaction code unique.
- Idempotency unique theo user + booking + key.
- Không lưu card/bank secret.
- `late_success` đánh dấu callback thành công sau khi booking hết hạn; không hồi sinh booking.

### `payment_refunds`

- Một refund active cho mỗi payment thành công.
- Lưu amount/currency, reason và lifecycle `PENDING/SUCCEEDED/FAILED`.
- Refund là audit record riêng; không xóa hoặc sửa lịch sử payment gốc.

### `payment_webhook_receipts`

Inbox/dedup callback provider. Chỉ lưu event ID, hash và kết quả xác minh/xử lý; không lưu raw payload mặc định.

### `outbox_events`

Transactional outbox cho notification/integration. Payload JSONB phải tối thiểu, version hóa và không chứa chat/note/payment secret.

## Index chính

| Query | Index |
|---|---|
| Slot trống của expert | `idx_expert_schedules_available_start` |
| Chống booking active trùng slot | `uq_bookings_active_schedule` |
| User booking history | `idx_bookings_user_history` |
| Expert booking history | `idx_bookings_expert_history` |
| Message history | `idx_messages_conversation_history` |
| Review của expert | `idx_expert_reviews_expert_created` (theo `expert_user_id`) |
| Payment history | `idx_payments_booking_created` |
| Refund pending/retry | `idx_payment_refunds_pending` |
| Outbox pending/retry | `idx_outbox_pending` |

## Migration

- `V1__Init_Booking_Schema.sql`: tạo schema, 10 bảng, constraint và index baseline; không có `expert_profiles`.
- Không sửa V1 sau khi migration được chạy ở môi trường dùng chung.
- Mọi thay đổi tiếp theo dùng `V2__...sql` trở đi.

## Kiểm thử

- Đã có Testcontainers test cho clean migration, 10 bảng, UUID/audit/time rule, hold/idempotency/refund constraint.
- Chưa thực thi trong lượt gần nhất do Docker không khả dụng.
- Cần bổ sung PostgreSQL concurrency test cho advisory lock/double-book và query ownership/keyset sau khi Docker chạy.
