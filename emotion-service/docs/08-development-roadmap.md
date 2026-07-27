# Lộ trình phát triển Emotion Service

## Nguyên tắc ưu tiên

Ưu tiên an toàn dữ liệu và capability độc lập trước thuật toán cảnh báo phức tạp. Mỗi phase phải tạo lát cắt chạy được, có contract và test; không xây đồng thời toàn bộ entity/controller rồi mới kiểm thử.

## Tiến độ ngày 2026-07-22

- Đã hoàn thành service layer cho journal, health metric, assessment và baseline risk; đã có V3 prerequisite và unit test rule chính.
- Đã có security, Jakarta Request DTO validation và HTTP error prerequisite; các feature ngoài journal chưa có controller nên chưa đạt trạng thái end-to-end/public API.
- Đã triển khai create/detail/history/soft-delete journal tại `/api/v1/emotion-journals` và trend ngày/tuần/tháng tại `/api/v1/emotion-trends`, gồm ownership, soft-delete filtering, bucket rỗng liên tục và mapping `emotion-v1`; không có update use case.
- Đã bổ sung verified `X-User-Role`, authority trong SecurityContext, bảo vệ `/api/v1/admin/**` bằng `ROLE_ADMIN` và triển khai Admin Assessment list có status filter/keyset pagination.
- Đã triển khai Admin list/detail/create/full-replace draft; admin chỉ nhập questions, server sinh response scale theo `AssessmentCode` và lưu option snapshot. Publish/archive cùng UC clone bản published thành draft version kế tiếp đã triển khai; core Admin Assessment catalog hiện đủ lifecycle `DRAFT -> PUBLISHED -> ARCHIVED`.
- Đã triển khai User Assessment published list/detail tại `GET /api/v1/assessments` và `GET /api/v1/assessments/{code}`, chỉ `ROLE_USER`; detail trả ordered questions/options nhưng không lộ score/status.
- Đã triển khai User Assessment submit tại `POST /api/v1/assessments/{code}/submissions`: bắt buộc idempotency key, chấm điểm từ catalog published hiện hành, lưu snapshot/version và trả kết quả sàng lọc ngay.
- Đã triển khai User Assessment result history/detail tại `GET /api/v1/assessment-results` và `GET /api/v1/assessment-results/{id}` với ownership, soft-delete filtering và keyset pagination.
- Đã có Testcontainers suite cho migration V1-V4, repository và service; baseline V1-V3 đã chạy xanh trên PostgreSQL 16.8 ngày 2026-07-23, V4 đang chờ Docker khả dụng để xác minh.
- Composite risk, crisis workflow, event/outbox, notification và consent/retention vẫn thuộc các phase production tiếp theo.

## Phase 0 - Làm sạch nền tảng

Mục tiêu: build lặp lại được và loại bỏ rủi ro cấu hình.

- Chuẩn hóa `application.yml` thành UTF-8 để khắc phục lỗi `maven-resources-plugin` đã quan sát ngày 2026-07-22.
- Sửa/xác minh Maven Wrapper, sau đó chạy `mvnw test/verify` và xác minh tổ hợp Java/Spring Boot/Spring Cloud/dependency thực sự tương thích.
- Thay credential mặc định trong `application.yml` bằng placeholder local an toàn; bổ sung profile/env documentation.
- Bổ sung Actuator, OpenAPI và dependency test cần thiết sau khi chốt version; Jakarta Validation cho Request DTO đã được triển khai.
- Xác minh Flyway V1 trên PostgreSQL rỗng; quyết định quyền tạo `uuid-ossp` và schema.
- Chốt timezone, error envelope, correlation ID, logging redaction.
- Xác nhận thiết kế PostgreSQL-only, schema ownership, index và chiến lược tăng trưởng dữ liệu.

Exit criteria: clean checkout build/test được, database bootstrap thành công, không có secret dùng chung trong source.

## Phase 1 - Nhật ký cảm xúc MVP

- Domain model và enum/catalog cảm xúc.
- Create/list/detail/soft-delete với ownership; không cho sửa journal, soft delete chỉ trong 15 phút.
- Pagination/time filtering và trend aggregation ngày/tuần/tháng — đã triển khai.
- Security integration với Gateway/JWT.
- Unit, controller, PostgreSQL integration và authorization tests.
- OpenAPI/examples.

Exit criteria: UC 2.1-2.3 chạy end-to-end, không đọc chéo user, query trend có index/query plan hợp lý.

## Phase 2 - Assessment catalog và scoring

- Migration cho version/status/risk thresholds và constraint thứ tự.
- Admin draft/publish/archive.
- User list/detail/submit/history/result detail đã triển khai.
- Scoring engine theo strategy/version; snapshot kết quả và idempotency.
- Seed bộ test chỉ sau khi nội dung và ngưỡng được chuyên gia phê duyệt.

Exit criteria: UC 3.1-3.2 và phần Emotion Service của UC 6.2; test boundary cho mọi threshold và tái tạo được kết quả cũ.

## Phase 3 - Health metric ingestion

- Chốt contract consent ownership và Health Connect/HealthKit payload.
- Migration cho external sample/idempotency/metadata cần thiết.
- Batch validation, unit normalization, dedup và query/trend.
- Giới hạn payload/rate, xử lý dữ liệu trễ/offline.
- Load test batch/query theo workload mục tiêu.

Exit criteria: UC 2.4-2.5 phía backend; retry không tạo trùng và dữ liệu sai đơn vị/thời gian bị từ chối rõ ràng.

## Phase 4 - Phân tích rủi ro và cảnh báo

- Workshops với chuyên gia lâm sàng/product về rule, disclaimer, escalation và crisis policy.
- Rule engine version hóa, `INSUFFICIENT_DATA`, confidence/reason codes.
- Migration alert explainability, dedup/cooldown.
- RabbitMQ + transactional outbox/inbox + DLQ.
- Integration notification template/FCM owner.
- Test hồi quy rule, false-positive review và safety red-team.

Exit criteria: UC 3.3-3.4 có phê duyệt chuyên môn, alert giải thích/replay được, không spam và không tuyên bố chẩn đoán.

## Phase 5 - Chia sẻ với chuyên gia và tích hợp hệ thống

- Chốt consent model và authorization proof với Auth/Booking Service.
- Shared summary tối thiểu cho UC 5.6, có expiry/revocation/audit.
- Event/contract tests giữa Emotion, Booking, Notification và AI.
- Tín hiệu tối thiểu cho UC 3.5; Booking/AI sở hữu việc chọn chuyên gia.

Exit criteria: expert chỉ xem đúng user/phạm vi/thời gian được phép; revoke có hiệu lực và truy cập được audit.

## Phase 6 - Production readiness

- SLO p95/p99 sau benchmark; capacity và HikariCP tuning.
- Dashboard/alert: HTTP, DB pool, query latency, outbox lag, consumer/DLQ.
- Encryption/key management, DB least privilege, dependency/container scanning.
- Backup/restore drill, RPO/RTO, retention/export/erasure.
- Chaos/failure tests cho DB, broker và downstream; runbook/on-call.
- Privacy/security/clinical review và rollout theo feature flag.

Exit criteria: đạt checklist vận hành và phê duyệt release production.

## Backlog quyết định chặn production

1. Format claim/service identity và authorization giữa service.
2. Ownership consent, notification record và privacy workflow.
3. Catalog/scoring/threshold được chuyên gia phê duyệt.
4. SLO, RPO, RTO, retention và data residency cho PostgreSQL.
5. Crisis escalation theo thị trường triển khai.

## Không nằm trong roadmap service

UI native xin quyền, AI RAG/streaming, expert chat, booking/payment và gửi FCM/email được triển khai ở service/capability tương ứng; Emotion Service chỉ cung cấp contract cần thiết.
