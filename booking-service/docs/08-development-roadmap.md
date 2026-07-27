# Lộ trình phát triển Booking Service

## Phase 0 - Foundation

- Cấu hình UTF-8, env secret, Flyway/JPA validate.
- SecurityContext verified identity/role, correlation ID, error envelope.
- Jakarta Validation, JPA Auditing, injected Clock.
- PostgreSQL Testcontainers cho V1.
- OpenAPI dependency và baseline.

Trạng thái: code foundation và Testcontainers đã có; PostgreSQL execution còn chờ Docker.

## Phase 1 - Auth expert contract và Schedule

- Chốt Auth API/event cho expert eligibility, public profile và fee/currency.
- Schedule entity/enum/repository/DTO/mapper/service và Controller đã có.
- Advisory lock theo `expertUserId`, overlap validation và public available-slot query.
- Test Auth adapter và concurrent schedule creation.

Exit: Booking không persist expert profile; concurrent writers không tạo slot overlap.

## Phase 2 - Booking core trước Controller

- Instant checkout, history/detail và cancellation policy đã có ở service.
- Expert inbox/detail, cancellation decision, cancel, complete và user no-show đã có ở service.
- Atomic booking/slot/outbox, keyset và ownership query đã thiết kế.
- Controller từng UC và MockMvc đã hoàn tất; Postman cases nằm tại `docs/11-postman-test-cases.md`.

Exit: không double-book; transition được kiểm thử đầy đủ.

## Phase 3 - Payment

- Chọn provider đầu tiên và sandbox.
- Payment attempt/idempotency, verified callback receipt và refund lifecycle core đã có.
- Cần chọn provider và viết adapter signature/checkout/refund.
- Secret management và provider contract test.

Exit: callback replay không tạo side effect kép; amount không đến từ client.

## Phase 4 - Consultation

- Conversation bootstrap, message history/send/read REST API đã có.
- WebSocket auth/authorization/delivery.
- Consultation note, complete, user review và rating aggregate API đã có.
- Attachment object-storage policy.

Exit: chỉ participant truy cập; history bền vững khi socket gián đoạn.

## Phase 5 - Integration/production readiness

- RabbitMQ outbox relay, retry/DLQ.
- Notification integration.
- Consent authorization với Emotion Service.
- Rate limiting, audit, encryption, retention/export/erasure.
- Load/security/chaos test, SLO/RPO/RTO.

## Thứ tự triển khai trong mỗi feature

1. Entity/enum theo migration.
2. Repository + PostgreSQL integration test.
3. Request/response DTO + validation.
4. MapStruct mapper.
5. Service interface.
6. Service implementation + unit test.
7. Security/error prerequisite.
8. Controller từng use case + MockMvc/Postman.
9. OpenAPI/docs/regression suite.
