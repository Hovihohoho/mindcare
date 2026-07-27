# Coding Style

## Java và Spring

- Java 17 là baseline cho đến khi POM/CI được nâng có chủ đích.
- 4 spaces, UTF-8, một public top-level type mỗi file; không wildcard import.
- Ưu tiên immutable value/DTO; dùng `record` cho transport value phù hợp.
- Constructor injection. Không dùng `@Autowired` trên field.
- Tránh `Optional` ở field/entity/DTO; dùng cho return type khi thực sự biểu đạt vắng mặt.
- Không bắt `Exception` chung rồi nuốt lỗi; map exception tại global API handler.
- Không trả `null` collection; trả collection rỗng khi contract cho phép.

## Naming

| Thành phần | Quy ước | Ví dụ |
|---|---|---|
| Package | lowercase, theo feature | `assessment.application` |
| Class/record/enum | PascalCase | `SubmitAssessmentCommand` |
| Method/field | camelCase | `calculateRiskLevel` |
| Constant/enum value | UPPER_SNAKE_CASE | `SLEEP_HOURS` |
| Request/response | hậu tố rõ | `CreateJournalRequest` |
| Use case/service | động từ + đối tượng | `SubmitAssessmentService` |
| DB | snake_case | `assessment_results` |
| Event | lower dot + version | `assessment.completed.v1` |

## API layer

- Controller chỉ xử lý HTTP, validation, principal và status/header.
- Dùng Jakarta Validation cho shape; collection/object lồng nhau phải có `@Valid` và constraint null/size phù hợp.
- Constraint transport chỉ biểu diễn rule đã được tài liệu/service xác nhận; invariant chéo field, catalog, ownership và state nằm trong application/domain.
- Rule giới hạn ký tự theo Unicode code point phải dùng validator code-point-aware; không dùng `@Size` khi semantics không phải UTF-16 code unit.
- Không expose entity, stack trace, raw SQL error hoặc field nội bộ.
- Endpoint self-service không nhận `userId` từ client.
- Controller dùng `@AuthenticationPrincipal AuthenticatedUser`, lấy UUID từ principal và truyền vào service; không khai báo lặp `@RequestHeader("X-User-Id")`.
- Pagination, sorting, timestamp và error code nhất quán với `05-api.md`.

## Domain và application

- Method domain dùng ngôn ngữ nghiệp vụ, không phải CRUD mơ hồ.
- Aggregate bảo vệ invariant; không để controller tự tính score/risk.
- Scoring/risk policy là pure/deterministic khi có thể, input chứa version rõ ràng.
- Transaction boundary ở application service. External network call không giữ DB transaction lâu; dùng outbox cho side effect async.
- Clock/ID generator/external client được inject để test deterministic.

## Persistence

- Entity JPA không dùng làm API model.
- UUID entity dùng `@GeneratedValue(strategy = GenerationType.UUID)`. Trường thời gian dùng `OffsetDateTime`; `created_at`/`updated_at` dùng `@CreatedDate`/`@LastModifiedDate` và `AuditingEntityListener`.
- Query luôn xét ownership và `deleted_at`; review kỹ method name query tự sinh.
- Tránh N+1 bằng projection/fetch có chủ đích; không bật eager toàn cục để chữa triệu chứng.
- Không dùng `ddl-auto` create/update. Mọi schema change qua Flyway.
- JSONB có typed serializer/schema/version và test round-trip.
- Optimistic locking (`@Version`) cho aggregate có concurrent update khi cần.

## Mapping và Lombok

- MapStruct cho mapping cơ học, compile-time. Mapping có quyết định nghiệp vụ viết rõ trong domain/application.
- Lombok dùng tiết chế; tránh `@Data` trên JPA entity vì equality/toString/lazy relation.
- Equality của entity dựa trên identity ổn định; không tự động gồm collection mutable.

## Error handling

- Exception nghiệp vụ cụ thể, ví dụ `AssessmentNotPublishedException`, `DuplicateSubmissionException`.
- Mã lỗi public ổn định và test được; message có thể localize.
- `404` không làm lộ việc resource của user khác tồn tại.
- Retry chỉ cho lỗi tạm thời và operation idempotent; không retry validation/business error.

## Log, metric và trace

- Structured log: event name, trace/correlation ID, opaque resource ID, outcome, duration.
- Không log JWT/refresh token, email/phone, journal content, assessment answers, raw metric hoặc retrieved sensitive context.
- Exception log một lần tại boundary phù hợp, tránh log lặp qua mọi layer.
- Metric label phải bounded; không dùng user/resource ID làm label.

## Test

- Tên test mô tả điều kiện và kết quả: `submit_rejectsOptionFromAnotherAssessment`.
- Unit test domain: happy path, boundary, invalid state, version behavior.
- Controller test: validation, auth/ownership, status/error schema.
- Repository/migration test trên PostgreSQL, ưu tiên Testcontainers.
- Contract test cho OpenAPI/event; integration test cho outbox/idempotency.
- Test data hoàn toàn giả, không lấy nhật ký hoặc dữ liệu sức khỏe thật.
- Test phải độc lập thời gian bằng injected clock và không phụ thuộc thứ tự.

## SQL và migration

- Keyword SQL uppercase, identifier snake_case; constraint/index có tên rõ.
- Migration nhỏ, forward-only sau khi chia sẻ; comment giải thích lý do, không lặp lại cú pháp hiển nhiên.
- Với backfill, nêu lock/runtime/rollback và tách khỏi DDL lớn nếu cần.
- Thêm index cùng query/use case chứng minh nhu cầu.

## Documentation và review

- Public type/method chỉ cần Javadoc khi contract/lý do không rõ từ tên; ưu tiên giải thích “vì sao”.
- TODO có owner/context hoặc issue; không để comment hứa hẹn mơ hồ.
- Khi đổi business rule/schema/API/event, cập nhật file docs tương ứng và `10-decisions.md` nếu là quyết định kiến trúc.
- Review đặc biệt cho access control, soft delete, idempotency, timezone, score threshold và dữ liệu nhạy cảm.
