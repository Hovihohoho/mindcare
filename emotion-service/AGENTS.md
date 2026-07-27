# Emotion Service - Hướng dẫn cho coding agent

## 1. Đọc trước khi thay đổi

Đọc theo thứ tự:

1. `docs/00-system-context.md` để hiểu MindCare và ranh giới service.
2. `docs/01-overview.md` và `docs/02-requirements.md` để hiểu phạm vi.
3. Tài liệu chuyên đề tương ứng trong `docs/03-10`.
4. Mã nguồn, migration và test hiện tại. Mã nguồn là bằng chứng cho trạng thái đã triển khai; roadmap/API đề xuất không phải bằng chứng tính năng đã tồn tại.

Khi các nguồn mâu thuẫn, ưu tiên theo thứ tự: quyết định đã được duyệt trong `docs/10-decisions.md`, contract liên service đã phát hành, migration đã chạy ở môi trường dùng chung, mã nguồn và test, sau đó mới đến tài liệu yêu cầu ban đầu. Không tự quyết một thay đổi ảnh hưởng contract hoặc dữ liệu; ghi rõ giả định và yêu cầu xác nhận.

## 2. Trạng thái repository hiện tại

- Java 17, Spring Boot, Spring MVC, Spring Data JPA, Flyway, PostgreSQL, MapStruct và Lombok.
- Service chạy mặc định ở cổng `8082`; Eureka hiện bị tắt trong cấu hình local.
- Hiện có application bootstrap, cấu hình, migration V1-V4, JPA entity/repository, DTO contract có Jakarta Validation, MapStruct mapper, service interface và implementation cho `journal`, `healthmetric`, `assessment`, `risk`; đã có pre-auth filter `X-User-Id`, SecurityContext, correlation ID và global HTTP error handler. Public API journal hiện đã triển khai create, detail, history phân trang và soft delete tại `/api/v1/emotion-journals`; journal không có update use case.
- Schema hiện tại là `emotion_schema`, gồm 8 bảng. V3 bổ sung `health_metric_sync_requests`, version/lifecycle assessment, idempotency, option ordering và metadata cảnh báo; V4 đảm bảo tối đa một assessment draft active cho mỗi code.
- Các tài liệu API và event trong `docs/05-api.md`, `docs/06-events.md` là contract mục tiêu, chưa phải contract đang chạy.
- Lần kiểm tra ngày 2026-07-23: đã bổ sung PostgreSQL Testcontainers 2.0.5 và 7 integration test cho migration V1→V3, repository và health service. `mvn.cmd verify` thành công với toàn bộ 20 test chạy xanh, không có test bị skip; migration V1→V3, upgrade có dữ liệu từ V1 và JPA query được xác minh trên PostgreSQL 16.8. `application.yml` là UTF-8 và không chứa mật khẩu hard-code. Maven Wrapper chưa được xác nhận hoạt động.
- Lần kiểm tra ngày 2026-07-24 sau khi bổ sung Jakarta Validation và mở rộng HTTP error handler: 18/18 test chuyên biệt chạy xanh; `mvn.cmd verify` build thành công với 38 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng trong lượt chạy này. Không diễn giải test bị skip là kết quả PostgreSQL mới.
- Lần kiểm tra ngày 2026-07-24 sau khi triển khai UC tạo nhật ký: controller integration test chạy xanh 5/5; `mvn.cmd verify` build thành công với 43 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.
- Lần kiểm tra ngày 2026-07-24 sau khi triển khai UC xem chi tiết nhật ký: journal controller integration test chạy xanh 9/9; `mvn.cmd verify` build thành công với 47 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.
- Lần kiểm tra ngày 2026-07-24 sau khi triển khai UC lịch sử journal phân trang: journal controller integration test chạy xanh 14/14; `mvn.cmd verify` build thành công với 52 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.
- Lần kiểm tra ngày 2026-07-24 sau khi triển khai UC soft delete journal: 24/24 test chuyên biệt service/controller chạy xanh; `mvn.cmd verify` build thành công với 59 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.
- UC thống kê/xu hướng journal đã expose `GET /api/v1/emotion-trends`, hỗ trợ bucket `DAY`, `WEEK`, `MONTH`, timezone IANA, chuỗi bucket liên tục và mapping điểm version `emotion-v1`.
- Lần kiểm tra ngày 2026-07-24 sau UC thống kê/xu hướng: 14/14 test chuyên biệt service/controller/bucket chạy xanh; `mvn.cmd verify` build thành công với 68 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.
- SecurityContext hiện nhận thêm verified `X-User-Role` (`ROLE_USER`, `ROLE_EXPERT`, `ROLE_ADMIN`); `/api/v1/admin/**` bắt buộc `ROLE_ADMIN`. Admin Assessment list đã expose `GET /api/v1/admin/assessments` với status filter và keyset pagination.
- Lần kiểm tra ngày 2026-07-24 sau verified role và Admin Assessment list: 19/19 test security/controller mục tiêu chạy xanh; `mvn.cmd verify` build thành công với 74 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng.
- Assessment code là enum application gồm `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`; JSON/URL/PostgreSQL vẫn dùng code canonical có dấu gạch ngang qua converter. Chỉ PHQ-9/GAD-7 hiện có scoring policy được phép publish.
- Lần kiểm tra ngày 2026-07-24 sau refactor `AssessmentCode`: 19/19 test hồi quy mục tiêu chạy xanh; `mvn.cmd verify` build thành công với 77 test, 0 failure/error và 7 PostgreSQL integration test bị skip vì Docker daemon không khả dụng. Không có migration mới.
- Admin Assessment đã có `POST /api/v1/admin/assessments`: tạo atomically assessment version 1 `DRAFT`; admin chỉ gửi questions, server sinh và persist ordered answer options theo `AssessmentDefinitionRegistry`. Draft cần `1..N` câu, publish cần đúng N và scale chuẩn; chỉ PHQ-9/GAD-7 hiện có scoring policy.
- Lần kiểm tra ngày 2026-07-24 sau UC Admin tạo assessment draft: 17/17 test service/controller mục tiêu chạy xanh; `mvn.cmd verify` build thành công với 87 test, 0 failure/error và 8 PostgreSQL integration test bị skip vì Docker daemon không khả dụng. Không có migration mới.
- Admin Assessment đã có `PUT /api/v1/admin/assessments/{id}`: chỉ full-replace aggregate `DRAFT`, giữ nguyên id/code/version/status, soft-delete questions/options cũ trong cùng transaction; không ngầm tạo version từ `PUBLISHED`.
- Lần kiểm tra ngày 2026-07-24 sau UC Admin cập nhật assessment draft: 24/24 test service/controller mục tiêu chạy xanh; `mvn.cmd verify` build thành công với 95 test, 0 failure/error và 9 PostgreSQL integration test bị skip vì Docker daemon không khả dụng. Không có migration mới.
- Lần kiểm tra ngày 2026-07-25 sau refinement server-generated assessment scale: 36/36 test definition/service/controller/DTO mục tiêu chạy xanh; `mvn.cmd test` thành công với 102 test, 0 failure/error và 9 PostgreSQL integration test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify` chưa chạy được do môi trường từ chối Maven Central; không có migration mới.
- Admin Assessment đã có `POST /api/v1/admin/assessments/{id}:publish` và `POST /api/v1/admin/assessments/{id}:archive`. Publish chỉ nhận draft đủ definition và scoring policy; archive chỉ nhận `PUBLISHED`, không soft-delete catalog/result.
- Lần kiểm tra ngày 2026-07-25 sau UC Admin publish/archive: 37/37 test service/controller mục tiêu chạy xanh; `mvn.cmd test` toàn bộ thành công với 112 test, 0 failure/error và 10 PostgreSQL integration test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công sau đó. Không có migration mới.
- Admin Assessment đã có `POST /api/v1/admin/assessments/{id}:create-next-version`: clone bản `PUBLISHED` thành draft version `max + 1`, giữ source immutable, sinh lại options theo definition và từ chối khi code đã có draft chờ. V4 thêm partial unique index bảo vệ invariant một draft active mỗi code.
- Lần kiểm tra ngày 2026-07-25 sau UC create-next-version: 45/45 test service/controller mục tiêu chạy xanh; `mvn.cmd test` toàn bộ thành công với 121 test, 0 failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Migration V4 và PostgreSQL lifecycle test mới chưa được thực thi trong lượt này.
- Admin Assessment đã có `GET /api/v1/admin/assessments/{id}`: trả aggregate đầy đủ của một version ở mọi lifecycle status, loại assessment/questions/options đã soft-delete và chỉ dành cho `ROLE_ADMIN`.
- Lần kiểm tra ngày 2026-07-25 sau UC Admin assessment detail: 50/50 test service/controller mục tiêu chạy xanh; `mvn.cmd test` toàn bộ thành công với 126 test, 0 failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.
- User Assessment đã có `GET /api/v1/assessments`: chỉ `ROLE_USER`, trả summary của các version `PUBLISHED` active theo code tăng dần; không trả questions/options/score/status.
- Lần kiểm tra ngày 2026-07-25 sau UC User published assessment list: 55/55 test assessment controller/service mục tiêu chạy xanh; `mvn.cmd test` toàn bộ thành công với 131 test, 0 failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.
- User Assessment đã có `GET /api/v1/assessments/{code}`: chỉ `ROLE_USER`, chỉ đọc version `PUBLISHED` active, trả ordered questions/options nhưng không trả `scoreValue` hoặc status; code canonical được parse qua web converter.
- Lần kiểm tra ngày 2026-07-25 sau UC User published assessment detail: 34/34 test user controller/service mục tiêu chạy xanh; `mvn.cmd test` toàn bộ thành công với 137 test, 0 failure/error và 11 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.
- User Assessment đã có `POST /api/v1/assessments/{code}/submissions`: chỉ `ROLE_USER`, bắt buộc `Idempotency-Key`, identity từ SecurityContext, chỉ nhận published version hiện hành và trả `201 Created` cùng kết quả chấm điểm/sàng lọc; không nhận score từ client.
- Lần kiểm tra ngày 2026-07-25 sau UC User submit assessment: 43/43 test controller/service mục tiêu chạy xanh; `mvn.cmd test` toàn bộ thành công với 147 test, 0 failure/error và 12 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. PostgreSQL integration test submission mới biên dịch thành công nhưng bị skip do Docker không khả dụng; không diễn giải đây là xác minh persistence mới. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.
- User Assessment result đã có `GET /api/v1/assessment-results` và `GET /api/v1/assessment-results/{id}`: chỉ `ROLE_USER`, identity từ SecurityContext, history dùng `[from,to)`/keyset pagination và detail áp dụng ownership cùng soft-delete filtering; foreign/deleted result trả 404 không phân biệt.
- Lần kiểm tra ngày 2026-07-25 sau UC User assessment result history/detail: 43/43 test controller/service mục tiêu chạy xanh; `mvn.cmd test` toàn bộ thành công với 160 test, 0 failure/error và 12 PostgreSQL Testcontainers test bị skip vì Docker daemon không khả dụng. PostgreSQL test history/detail mở rộng chưa thực thi do Docker không khả dụng. `mvn.cmd verify -DskipTests` package thành công. Không có migration mới.

## 3. Ranh giới nghiệp vụ

Service này sở hữu:

- nhật ký cảm xúc và thống kê/xu hướng cảm xúc;
- dữ liệu sức khỏe do client đồng bộ;
- danh mục bài đánh giá, câu hỏi, lựa chọn và cách chấm điểm;
- kết quả đánh giá của người dùng;
- phân tích rủi ro tổng hợp và log cảnh báo có thể giải thích.

Service này không sở hữu tài khoản/JWT, hồ sơ chuyên gia, booking, thanh toán, chat chuyên gia, chatbot RAG hay việc gửi push/email. Chỉ tham chiếu `userId`/`expertId` từ service khác và giao tiếp qua contract. Không tạo foreign key xuyên service/schema.

## 4. Quy tắc triển khai bắt buộc

- Lấy danh tính người dùng từ JWT/verified headers do Gateway cung cấp; không tin `userId` do client tự gửi cho thao tác của chính họ.
- Mọi truy vấn dữ liệu người dùng phải áp dụng ownership/authorization và mặc định loại bản ghi có `deleted_at`.
- Không đưa chẩn đoán y khoa vào response. Kết quả và cảnh báo phải dùng ngôn ngữ sàng lọc/hỗ trợ, có lý do và hành động an toàn.
- Thuật toán tính điểm và ngưỡng rủi ro phải có version, kiểm thử biên và khả năng truy vết. Không thay đổi công thức đã công bố mà không có migration/decision và test hồi quy.
- Dữ liệu thời gian lưu theo UTC; API dùng ISO-8601 có offset. Quy ước timezone hiển thị thuộc client.
- Entity ánh xạ trường thời gian bằng `OffsetDateTime`; database dùng `TIMESTAMPTZ`. UUID sinh ở application bằng `GenerationType.UUID`. `created_at`/`updated_at` do Spring Data JPA Auditing quản lý, không dùng default/trigger phía PostgreSQL.
- API mới dùng `/api/v1`, DTO riêng; không trả JPA entity trực tiếp. Validate ở biên HTTP và kiểm tra invariant tại domain/application layer.
- Với batch đồng bộ sức khỏe, thiết kế idempotent. Event consumer cũng phải idempotent và chỉ acknowledge sau khi transaction cần thiết thành công.
- Event không chứa nội dung nhật ký, câu trả lời chi tiết hoặc dữ liệu sức khỏe không cần thiết. Dùng ID và metadata tối thiểu.
- Không sửa migration đã chạy ở môi trường dùng chung. Tạo `V2__...sql` trở đi. Với migration mới ở repository chưa chia sẻ, vẫn nêu rõ thay đổi trong handoff.
- PostgreSQL là database engine nghiệp vụ duy nhất. Mọi persistence mới phải dùng `emotion_schema` và Flyway; không thêm database engine khác hoặc dual-write. Redis chỉ có thể là cache/dữ liệu tạm thời.
- Không ghi secret thật vào repository. Cấu hình nhạy cảm phải đến từ biến môi trường/secret store; giá trị mặc định chỉ dành cho local và không được là credential dùng chung.
- Giữ encoding UTF-8 cho mã nguồn, SQL và Markdown.

## 5. Cấu trúc và phong cách

Repository bắt buộc sử dụng package theo feature. Các feature hiện tại là `journal`, `healthmetric`, `assessment`, `risk`; thành phần dùng chung nằm trong `shared`. Trong mỗi feature, tạo các package con `controller`, `dto`, `entity`, `mapper`, `repository`, `service` khi thực sự cần. Không tạo lại các package layer dùng chung ở cấp `com.mindcare.emotionservice`.

- File mới phải nằm dưới feature sở hữu nghiệp vụ, ví dụ `journal/entity`, `journal/repository`, `journal/dto`.
- Chỉ đặt trong `shared` khi thành phần thật sự được ít nhất hai feature dùng và không chứa luật nghiệp vụ riêng của feature.
- Feature không truy cập repository/entity nội bộ của feature khác để ghép nghiệp vụ. Tích hợp chéo feature phải qua service contract rõ ràng hoặc event khi được triển khai.

- Class/interface: `PascalCase`; method/field: `camelCase`; constant và enum value: `UPPER_SNAKE_CASE`.
- DTO: hậu tố `Request`, `Response`; repository interface: hậu tố `Repository`; exception nghiệp vụ phải có tên cụ thể.
- Constructor injection; tránh field injection.
- Controller mỏng, transaction đặt ở application service/use case, repository chỉ xử lý persistence.
- MapStruct dùng cho mapping cơ học; mapping chứa luật nghiệp vụ phải nằm trong domain/application code.
- Log có cấu trúc với correlation/trace ID; không log nội dung nhật ký, câu trả lời assessment, token hay dữ liệu sức khỏe chi tiết.

Chi tiết tại `docs/07-project-structure.md` và `docs/09-coding-style.md`.

## 6. Kiểm thử và lệnh chuẩn

Trên Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

Nếu wrapper chưa hoạt động nhưng máy có Maven tương thích, có thể dùng `mvn.cmd test` để chẩn đoán. Không coi đây là thay thế lâu dài cho wrapper tái lập được.

Mỗi thay đổi nghiệp vụ cần tối thiểu unit test cho happy path, validation/invariant và boundary. API cần controller/integration test; persistence/migration cần test với PostgreSQL tương thích production, ưu tiên Testcontainers khi được bổ sung. Không tuyên bố hoàn tất nếu chỉ `contextLoads` chạy.

## 7. Definition of Done

- Phạm vi đúng ranh giới service và không làm lộ dữ liệu người dùng.
- Test liên quan chạy thành công; nêu rõ test nào chưa chạy và lý do.
- Migration/API/event tương thích ngược hoặc có kế hoạch versioning.
- OpenAPI và tài liệu trong `docs/` được cập nhật khi contract, schema, rule hoặc decision thay đổi.
- Không commit file build, secret, log hoặc dữ liệu sức khỏe/tâm lý mẫu có thể nhận diện cá nhân.
