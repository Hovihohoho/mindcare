# Coding Style Booking Service

## Java

- Java 17; class/interface `PascalCase`, method/field `camelCase`, constant/enum `UPPER_SNAKE_CASE`.
- Constructor injection; không field injection.
- DTO dùng Java record khi phù hợp, hậu tố `Request`/`Response`.
- Entity không dùng public setter đại trà; transition qua method có tên nghiệp vụ.
- `OffsetDateTime` cho thời gian; `BigDecimal` cho tiền.
- Không dùng `LocalDateTime` cho thời điểm tuyệt đối hoặc `double` cho money.
- Collection response/request được copy bất biến khi phù hợp.

## Persistence

- Entity map rõ `booking_schema`.
- UUID: `@GeneratedValue(strategy = GenerationType.UUID)`.
- Auditing: `@CreatedDate`, `@LastModifiedDate`, `AuditingEntityListener`.
- Association mặc định `LAZY`; tránh cascade rộng và N+1.
- Query user/expert phải có ownership và `deletedAt IS NULL`.
- Write aggregate dùng transaction; concurrent lifecycle dùng optimistic/pessimistic lock theo thiết kế.

## API

- `/api/v1`, DTO riêng, không trả entity.
- Jakarta Validation ở HTTP boundary; invariant kiểm tra lại ở service.
- Identity từ SecurityContext, không từ request body/query.
- Time ISO-8601 có offset; range `[from,to)`.
- Keyset cursor opaque; limit mặc định 20, tối đa 100.
- Create trả `201 + Location`; delete/cancel action trả semantics được contract hóa.

## Exception

- Exception nghiệp vụ có code ổn định: `SCHEDULE_NOT_AVAILABLE`, `INVALID_STATE_TRANSITION`, `PAYMENT_IDEMPOTENCY_CONFLICT`.
- 404 không phân biệt foreign/missing/deleted resource.
- Không trả stack trace, SQL/provider raw error.
- Mapping HTTP tập trung tại `GlobalExceptionHandler`.

## Mapping

- MapStruct `componentModel = "spring"`.
- `unmappedTargetPolicy = ReportingPolicy.ERROR`.
- Mapper không query repository, authorize, transition hoặc tính tiền/rating.

## Logging

- Structured event name, trace ID, opaque resource ID, outcome.
- Không log note đặt lịch, message, consultation note, JWT, payment callback/signature hoặc URL nhạy cảm.
- Không dùng user/resource ID làm metric label.

## Test

- Unit: happy path, boundary, invalid lifecycle, ownership, idempotency.
- Controller: validation, role, HTTP status/error schema.
- PostgreSQL Testcontainers: migration/query/constraint/concurrency.
- Payment: provider sandbox/fixture signature và replay.
- WebSocket: handshake auth, participant authorization, reconnect/history.
- Test dùng Clock cố định và dữ liệu giả.

## SQL

- Keyword uppercase, identifier `snake_case`.
- Constraint/index có tên.
- Migration forward-only sau khi chia sẻ.
- Không sửa V1 đã chạy; thêm V2+.
