# Booking Service - Hướng dẫn cho coding agent

## 1. Đọc trước khi thay đổi

Đọc theo thứ tự:

1. `docs/00-system-context.md`.
2. `docs/01-overview.md` và `docs/02-requirements.md`.
3. Tài liệu liên quan trong `docs/03-10`.
4. Source, migration và test hiện tại.

Mã nguồn/test là bằng chứng tính năng đã triển khai; API/roadmap đề xuất không phải bằng chứng endpoint đang chạy.

Khi nguồn mâu thuẫn, ưu tiên: decision accepted, contract liên service đã phát hành, migration đã chạy môi trường dùng chung, source/test, rồi tài liệu yêu cầu ban đầu. Không tự quyết thay đổi contract/dữ liệu khi rule còn được đánh dấu OPEN.

## 2. Trạng thái repository

- Java 17, Spring Boot 4.1.0, Spring Cloud 2025.1.2.
- Dependency hiện có: Spring MVC, Spring Data JPA, Flyway, PostgreSQL, MapStruct, Lombok, Eureka client.
- Local port `8083`; Eureka mặc định tắt.
- `application.yml` dùng PostgreSQL `booking_schema`, env-based password, JPA validate và Flyway.
- Migration V1 tạo 10 bảng: schedule, booking, conversation/message, review, consultation note, payment, refund, webhook receipt và outbox.
- Đã có feature-based entity/enum, repository, DTO có Jakarta Validation, MapStruct mapper, service interface/implementation, JPA Auditing, policy config, scheduler hết hạn, verified-header SecurityContext, correlation ID và global error handler. Chưa có business Controller, WebSocket adapter, RabbitMQ relay, Auth expert adapter hoặc payment-provider adapter thực.
- Tất cả endpoint trong `docs/05-api.md` và event trong `docs/06-events.md` đang là contract mục tiêu.
- Đã có PostgreSQL Testcontainers cho context/migration; clean migration test chưa chạy vì Docker daemon không khả dụng.
- Gateway hiện route booking/expert và emotion sai port so với trạng thái service; không tuyên bố end-to-end trước khi Gateway được sửa ở task riêng.
- Lần kiểm tra ngày 2026-07-25 sau khi hoàn thiện các lớp trước Controller: `mvn.cmd test` thành công với 16 test, 0 failure/error và 4 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. 12 unit test đã chạy xanh; migration V1/JPA query chưa được thực thi trên PostgreSQL trong lượt này.
- Lần chẩn đoán runtime ngày 2026-07-25: PostgreSQL local tại `localhost:5432` phản hồi; JPA ban đầu fail do Entity suy luận `currency` là `VARCHAR(3)` trong khi schema dùng `CHAR(3)`. Sau khi thêm `@JdbcTypeCode(SqlTypes.CHAR)` cho Booking/Payment/Refund, ứng dụng khởi động ổn định với Flyway tắt và cổng ngẫu nhiên; `mvn.cmd verify` tiếp tục thành công. `application.yml` đã được đưa lại về env-based secret.
- Lần kiểm tra Controller ngày 2026-07-25: đã có 27 request mappings cho expert directory, schedule, booking hai phía, REST chat, consultation note và review. `mvn.cmd verify` thành công với 48 test, 0 failure/error và 4 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng; package thành công. Payment API/provider và WebSocket chưa triển khai.
- Local/Postman có adapter expert giả định danh qua `BOOKING_AUTH_ADAPTER_MODE=local` và có thể tắt payment rõ ràng bằng `BOOKING_PAYMENT_REQUIRED=false`; payment mặc định vẫn bật. Không dùng hai chế độ local này cho production.

## 3. Ranh giới nghiệp vụ

Booking Service sở hữu:

- schedule/slot;
- booking lifecycle;
- payment attempt/callback state;
- conversation/message giữa user và expert;
- consultation note và review;
- authorization fact về quan hệ tư vấn;
- outbox event của các aggregate trên.

Không sở hữu:

- account, password, JWT, role, account status và toàn bộ expert profile/catalog;
- journal, health metric, assessment/risk;
- AI chat/RAG;
- notification delivery;
- object binary hoặc payment credentials.

Chỉ lưu external IDs/projection tối thiểu; không FK xuyên service/schema.

## 4. Quy tắc kỹ thuật bắt buộc

- PostgreSQL là database nghiệp vụ duy nhất; schema `booking_schema`.
- UUID sinh tại application bằng `GenerationType.UUID`; migration không UUID default.
- Database time dùng `TIMESTAMPTZ`; Java dùng `OffsetDateTime`.
- `created_at`/`updated_at` dùng Spring Data JPA Auditing; không database default/trigger.
- Money dùng `BigDecimal`/`NUMERIC`, không `double`.
- Mọi read dữ liệu actor phải có ownership/participant authorization và `deleted_at IS NULL`.
- Booking/schedule/payment lifecycle thay đổi trong transaction; chống race bằng advisory lock/row lock/version/constraint đã thiết kế.
- Amount, currency, owner và status là server-owned, không tin client.
- Payment create/callback và event consumer idempotent.
- Không log chat content, booking note, consultation note, token, payment raw callback/signature/secret.
- API `/api/v1`, DTO riêng, Jakarta Validation ở boundary và invariant ở service.
- Identity từ JWT/verified Gateway headers vào SecurityContext; không nhận `userId` của chính actor từ request.
- Migration đã dùng chung là forward-only; không sửa V1 sau khi áp dụng, tạo V2+.
- Secret từ env/secret store; không hard-code credential.
- UTF-8 cho Java/SQL/YAML/Markdown.

## 5. Cấu trúc

Package theo feature dưới `com.mindcare.bookingservice`:

```text
schedule
booking
payment
chat
consultation
review
integration
shared
```

Trong feature chỉ tạo `controller`, `dto`, `entity`, `mapper`, `repository`, `service` khi cần. Không tạo package layer dùng chung ở root.

- Constructor injection.
- Controller/WebSocket adapter mỏng.
- Transaction ở application service.
- Repository chỉ persistence.
- MapStruct chỉ mapping cơ học, bật unmapped target error.
- Feature không truy cập repository/entity nội bộ feature khác; dùng service contract/facade.

## 6. Quy trình phát triển

Trong mỗi feature:

1. Đối chiếu migration và business rule.
2. Entity/enum.
3. Repository + PostgreSQL integration test.
4. DTO + Jakarta Validation.
5. Mapper.
6. Service interface.
7. Service implementation + unit test.
8. Security/error prerequisite.
9. Controller từng UC + MockMvc/Postman.
10. OpenAPI/docs/full regression.

Dừng và báo nếu implementation cần một quyết định OPEN như payment provider/signature, Auth expert contract hoặc consent. Cancellation cutoff và chat window đã được chốt tại DEC-011.

## 7. Kiểm thử

Lệnh chuẩn:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Mỗi thay đổi nghiệp vụ cần happy path, validation/invariant, boundary, ownership/role và concurrency/idempotency khi liên quan. Persistence/migration phải test PostgreSQL production-compatible, ưu tiên Testcontainers. Test bị skip không phải bằng chứng PostgreSQL chạy thành công.

## 8. Definition of Done

- Đúng ranh giới và không lộ dữ liệu actor khác.
- Lifecycle/transaction/concurrency được test.
- Test liên quan xanh; nêu test chưa chạy và lý do.
- Migration/API/event tương thích hoặc có versioning.
- Docs/OpenAPI/decision cập nhật khi contract/rule/schema đổi.
- Không commit build output, IDE state, secret, raw payment payload hoặc dữ liệu nhạy cảm có thể nhận diện.
