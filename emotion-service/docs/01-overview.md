# Tổng quan Emotion Service

## Vai trò

`emotion-service` biến các tín hiệu do người dùng chủ động cung cấp hoặc đồng bộ từ thiết bị thành lịch sử có cấu trúc, kết quả sàng lọc và cảnh báo hỗ trợ có thể giải thích.

Service tập trung vào bốn capability:

1. **Emotion journal**: ghi nhận, tra cứu và tổng hợp xu hướng cảm xúc.
2. **Health metric ingestion**: nhận dữ liệu giấc ngủ, nhịp tim, bước chân từ client sau khi client có consent của hệ điều hành.
3. **Psychological assessment**: cung cấp bộ câu hỏi, chấm điểm, phân loại và lưu lịch sử kết quả.
4. **Risk analysis**: kết hợp tín hiệu theo cửa sổ thời gian, tạo log cảnh báo và yêu cầu hệ thống thông báo thực thi.

## Phạm vi

### Trong phạm vi

- Use case 2.1-2.5: nhật ký cảm xúc và dữ liệu sức khỏe.
- Use case 3.1-3.4: bài đánh giá, kết quả, phân tích tổng hợp và cảnh báo.
- Phần dữ liệu cho use case 5.6: expert xem dữ liệu user khi có authorization/consent hợp lệ.
- Phần quản trị bộ đánh giá của use case 6.2.
- Cung cấp tín hiệu rủi ro tối thiểu cho capability gợi ý chuyên gia/notification.

### Ngoài phạm vi

- Cấp quyền Health Connect/HealthKit ở UI/native layer.
- Xác thực tài khoản, JWT, OTP, role và hồ sơ user.
- Hồ sơ chuyên gia, tìm kiếm chuyên gia, lịch, booking, thanh toán và chat expert.
- AI chat, RAG, intent detection bằng LLM và lịch sử AI.
- Gửi FCM/email trực tiếp. Emotion Service chỉ phát intent/event thông báo.

## Trạng thái hiện tại (as-is)

| Hạng mục | Trạng thái |
|---|---|
| Bootstrap Spring Boot | Có |
| PostgreSQL + Flyway | Đã cấu hình |
| Schema và 8 bảng | V1 tạo 7 bảng; V2 chuẩn hóa UUID/`TIMESTAMPTZ`; V3 bổ sung idempotency, assessment version/lifecycle và alert metadata; V4 đảm bảo một assessment draft active mỗi code; V3/V4 chưa được xác nhận ở môi trường dùng chung |
| Entity/repository/application service | Đã có cho journal, health metric, assessment và risk baseline |
| REST controller/OpenAPI | Đã có create/detail/history/soft-delete và trend journal; Admin Assessment catalog đầy đủ; User Assessment published list/detail/submit/result history/result detail đã có; các feature endpoint khác và OpenAPI chưa có |
| RabbitMQ/outbox/consumer | Chưa có |
| Security ở service | Đã có stateless pre-auth filter đọc verified `X-User-Id` và `X-User-Role`, principal/authority trong SecurityContext, RBAC `ROLE_ADMIN` và JSON 401/403 |
| Test | Có unit/context, 11 PostgreSQL Testcontainers integration test, 6 Request DTO validation test, 13 security/error integration test và 19 journal controller integration test |

Assessment catalog hiện dùng enum application `AssessmentCode` cho năm loại `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`; PostgreSQL và API vẫn giữ code canonical dạng chuỗi.

`AssessmentDefinitionRegistry` hiện sở hữu số câu và response scale của năm code. Admin create/update chỉ gửi questions; server sinh options và lưu snapshot. Draft nhận `1..N` câu; publish yêu cầu đúng N, scale khớp definition và scoring policy đã được phê duyệt. Từ bản `PUBLISHED`, admin có thể clone sang draft version kế tiếp để chỉnh sửa mà không tác động catalog đang phục vụ; mỗi code chỉ có tối đa một draft chờ. Archive chỉ nhận assessment đang `PUBLISHED`.

### Trạng thái build đã biết

Ngày 2026-07-23, `mvn.cmd verify` thành công với 20/20 test xanh trên PostgreSQL Testcontainers 16.8, không có test bị skip. Suite bao phủ clean migration V1-V3, upgrade có dữ liệu V1, schema constraint/type/default, repository ownership/soft-delete/lifecycle/keyset pagination và health service atomic/idempotency. Maven Wrapper chưa được xác nhận hoạt động.

Ngày 2026-07-24, 18/18 test chuyên biệt cho Request DTO validation và security/error chạy xanh. Lượt `mvn.cmd verify` sau thay đổi build thành công với 38 test, không có failure/error và 7 PostgreSQL integration test bị skip do Docker daemon không khả dụng; PostgreSQL suite gần nhất chạy đầy đủ vẫn là kết quả ngày 2026-07-23.

Sau khi triển khai UC tạo nhật ký ngày 2026-07-24, controller integration test chạy xanh 5/5. `mvn.cmd verify` build thành công với 43 test, không có failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon vẫn không khả dụng.

Sau khi triển khai UC xem chi tiết nhật ký ngày 2026-07-24, journal controller integration test chạy xanh 9/9. `mvn.cmd verify` build thành công với 47 test, không có failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.

Sau khi triển khai UC lịch sử journal phân trang ngày 2026-07-24, journal controller integration test chạy xanh 14/14. `mvn.cmd verify` build thành công với 52 test, không có failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.

Sau khi triển khai UC soft delete journal ngày 2026-07-24, 24/24 test chuyên biệt service/controller chạy xanh. `mvn.cmd verify` build thành công với 59 test, không có failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.

Sau khi triển khai UC thống kê/xu hướng journal ngày 2026-07-24, 14/14 test chuyên biệt service/controller/bucket chạy xanh. `mvn.cmd verify` build thành công với 68 test, không có failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.

Sau khi bổ sung verified role và Admin Assessment list ngày 2026-07-24, 19/19 test security/controller mục tiêu chạy xanh. `mvn.cmd verify` build thành công với 74 test, không có failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.

Sau khi refactor `AssessmentCode` ngày 2026-07-24, 19/19 test hồi quy mục tiêu chạy xanh. `mvn.cmd verify` build thành công với 77 test, không có failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng; không có migration mới.

Sau khi triển khai UC Admin tạo assessment draft ngày 2026-07-24, 17/17 test service/controller mục tiêu chạy xanh. `mvn.cmd verify` build thành công với 87 test, không có failure/error và 8 PostgreSQL integration test bị skip vì Docker daemon không khả dụng; không có migration mới.

Sau khi triển khai UC Admin cập nhật assessment draft ngày 2026-07-24, 24/24 test service/controller mục tiêu chạy xanh. `mvn.cmd verify` build thành công với 95 test, không có failure/error và 9 PostgreSQL integration test bị skip vì Docker daemon không khả dụng; không có migration mới.

Sau refinement server-generated assessment scale ngày 2026-07-25, 36/36 test definition/service/controller/DTO mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 102 test, không có failure/error và 9 PostgreSQL integration test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify` chưa chạy được vì môi trường từ chối Maven Central; không có migration mới.

Sau UC Admin publish/archive ngày 2026-07-25, 37/37 test service/controller mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 112 test, không có failure/error và 10 PostgreSQL integration test bị skip vì Docker daemon không khả dụng; lifecycle test mới chưa được xác minh thực thi trên PostgreSQL trong lượt này. `mvn.cmd verify -DskipTests` package thành công sau đó. Không có migration mới.

Sau UC create-next-version ngày 2026-07-25, 45/45 test service/controller mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 121 test, không có failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Vì vậy migration V4 và lifecycle test versioning mới chưa được thực thi trên PostgreSQL trong lượt này.

Sau UC Admin assessment detail ngày 2026-07-25, 50/50 test service/controller mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 126 test, không có failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.

Sau UC User published assessment list ngày 2026-07-25, 55/55 test assessment controller/service mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 131 test, không có failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.

Sau UC User published assessment detail ngày 2026-07-25, 34/34 test user controller/service mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 137 test, không có failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.

Sau UC User submit assessment ngày 2026-07-25, 43/43 test controller/service mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 147 test, không có failure/error và 12 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. PostgreSQL integration test mới cho scoring/persistence/idempotency đã biên dịch nhưng bị skip cùng suite, nên chưa được diễn giải là xác minh persistence mới. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.

Sau UC User xem lịch sử và chi tiết kết quả assessment ngày 2026-07-25, 43/43 test controller/service mục tiêu chạy xanh. `mvn.cmd test` toàn bộ thành công với 160 test, không có failure/error và 12 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. PostgreSQL test đã được mở rộng để kiểm tra history/detail ownership nhưng chưa thực thi do Docker không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.

## Stack quan sát từ repository

- Java 17.
- Spring Boot `4.1.0`, Spring Cloud BOM `2025.1.2` theo `pom.xml`.
- Spring MVC, Spring Data JPA, Flyway, PostgreSQL.
- MapStruct `1.5.5.Final`, Lombok `1.18.30`.
- Port local `8082`, application name `emotion-service`.
- Eureka client có dependency nhưng đang `enabled: false`.

Các version trên mô tả cấu hình hiện tại, không phải tuyên bố rằng tổ hợp đã được xác minh tương thích. Việc sửa encoding/wrapper và chạy lại build/test là gate của roadmap.

## Mục tiêu chất lượng

- Đúng và truy vết được: mỗi kết quả assessment biết phiên bản công thức/ngưỡng.
- Riêng tư theo mặc định: ownership, consent và data minimization ở mọi đường đọc/ghi.
- Idempotent ở đường ingest và consume event.
- Quan sát được mà không log dữ liệu nhạy cảm.
- Contract rõ ràng, version hóa và tương thích ngược.
- Không biến điểm sàng lọc thành chẩn đoán y khoa.

## Điều hướng tài liệu

- Yêu cầu: [02-requirements.md](02-requirements.md)
- Luật nghiệp vụ: [03-business-rules.md](03-business-rules.md)
- Cơ sở dữ liệu: [04-database.md](04-database.md)
- API: [05-api.md](05-api.md)
- Event: [06-events.md](06-events.md)
- Cấu trúc mã: [07-project-structure.md](07-project-structure.md)
- Roadmap: [08-development-roadmap.md](08-development-roadmap.md)
- Coding style: [09-coding-style.md](09-coding-style.md)
- Quyết định: [10-decisions.md](10-decisions.md)
