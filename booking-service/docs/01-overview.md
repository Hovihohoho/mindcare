# Tổng quan Booking Service

## Capability

1. **Scheduling**: slot tư vấn của expert, chống trùng lịch và lifecycle slot.
2. **Booking**: instant checkout, hủy/duyệt yêu cầu hủy, hoàn tất và lịch sử hai phía.
3. **Payment**: payment attempt idempotent, callback/IPN và đồng bộ trạng thái booking.
4. **Consultation**: conversation/message, consultation note và review.
5. **Integration**: đọc expert profile từ Auth, xác nhận quan hệ tư vấn cho chia sẻ dữ liệu và phát outbox event.

## Trạng thái hiện tại

| Hạng mục | Trạng thái |
|---|---|
| Spring Boot bootstrap | Có |
| Java/Spring | Java 17, Spring Boot 4.1.0, Spring Cloud 2025.1.2 |
| Persistence dependency | JPA, Flyway, PostgreSQL |
| Cấu hình | `application.yml`, port local `8083`, env-based DB secret |
| Schema | V1 tạo `booking_schema` và 10 bảng; expert profile thuộc Auth Service |
| Entity/repository/DTO/mapper/service | Đã triển khai |
| Security foundation | Đã có verified headers, SecurityContext, correlation ID và error handler |
| Controller | Đã có cho expert directory, schedule, booking, REST chat, consultation note và review |
| WebSocket/RabbitMQ | Chưa triển khai |
| Testcontainers | Đã bổ sung; lượt gần nhất skip do Docker không khả dụng |
| Public API | Các API không liên quan payment đã có và được kiểm thử |

## Trạng thái kiểm tra ngày 2026-07-25

- `mvn.cmd verify -DskipTests` package thành công.
- Sau quyết định chuyển expert profile sang Auth rồi bổ sung refund aggregate, baseline V1 hiện có 10 bảng và không chứa `expert_profiles`.
- PostgreSQL local tại port `5432` có phản hồi.
- Docker client có nhưng daemon không khả dụng.
- Clean migration V1 bằng Testcontainers chưa được thực thi do Docker không khả dụng.
- JPA schema validation trên database local đã thành công sau khi map rõ PostgreSQL `CHAR(3)` cho ba trường currency; lượt xác minh dùng Flyway tắt để không tự ý thay đổi schema.

## Bảng V1

- `expert_schedules`
- `bookings`
- `conversations`
- `messages`
- `expert_reviews`
- `consultation_notes`
- `payments`
- `payment_refunds`
- `payment_webhook_receipts`
- `outbox_events`

## Trạng thái kiểm tra lớp trước Controller

- `mvn.cmd test`: 16 test, 0 failure/error, 4 PostgreSQL tests skip do Docker không khả dụng.
- Entity/repository/DTO/mapper/service/security foundation biên dịch thành công.
- Auth expert gateway và payment provider gateway hiện là port fail-closed; cần adapter contract thực trước khi chạy checkout end-to-end.

## Cấu hình local

Service dùng database của Docker Compose hiện tại:

```text
jdbc:postgresql://localhost:5432/mindcare_db?currentSchema=booking_schema
```

Biến môi trường bắt buộc:

```powershell
$env:DB_PASSWORD="<local-postgres-password>"
$env:BOOKING_AUTH_ADAPTER_MODE="local"
$env:BOOKING_PAYMENT_REQUIRED="false"
```

`BOOKING_AUTH_ADAPTER_MODE=local` bật catalog expert giả định danh. `BOOKING_PAYMENT_REQUIRED=false` chỉ dành cho local/test; mặc định là `true`.

## Trạng thái kiểm tra Controller ngày 2026-07-25

- Đã triển khai 27 request mappings cho expert directory, schedule, booking hai phía, REST chat, consultation note và review.
- `mvn.cmd verify`: 48 test, 0 failure/error; 4 PostgreSQL Testcontainers test skip do Docker daemon không khả dụng; package thành công.
- Unit test xác minh payment-bypass confirm booking và không gọi PaymentService.

## Mục tiêu chất lượng

- Không double-book dưới request đồng thời.
- Mọi query user/expert có ownership/participant authorization.
- Giá và currency từ Auth được snapshot tại thời điểm booking; Booking chỉ lưu external `expert_user_id`.
- Callback payment và event delivery idempotent.
- Thời gian dùng UTC/`TIMESTAMPTZ`; API dùng ISO-8601 có offset.
- Không log nội dung chat, consultation note, token, callback secret hoặc dữ liệu tâm lý.
