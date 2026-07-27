# Nhật ký quyết định kiến trúc

## Cách dùng

Tài liệu này ghi baseline hiện tại và các quyết định còn mở. Trạng thái:

- **Observed**: sự thật quan sát từ repository/tài liệu, không phải lựa chọn mới.
- **Provisional**: baseline để tiếp tục thiết kế, cần nhóm xác nhận trước khi tạo phụ thuộc khó đảo ngược.
- **Accepted**: đã được người có thẩm quyền dự án duyệt; cần ghi người/ngày/link.
- **Superseded**: đã được quyết định khác thay thế.

Không chuyển `Provisional` thành `Accepted` nếu chưa có phê duyệt thực tế.

## DEC-001 - Ranh giới Emotion Service

- Trạng thái: **Provisional**
- Ngày ghi nhận: 2026-07-22
- Quyết định: Emotion Service sở hữu journal, health metric ingestion, assessment catalog/result và risk alert log. Auth, booking/expert/payment/chat, AI RAG và notification delivery thuộc service khác.
- Lý do: khớp phân rã microservice trong đề tài và gom các capability cần cùng dữ liệu/rule phân tích.
- Hệ quả: chỉ lưu external IDs; không FK/join trực tiếp bảng của service khác. Shared expert view cần contract authorization.

## DEC-002 - Chỉ sử dụng PostgreSQL cho Emotion Service

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-22
- Người duyệt: chủ dự án, qua chỉ thị trực tiếp chỉ sử dụng PostgreSQL.
- Quyết định: PostgreSQL `emotion_schema` là cơ sở dữ liệu nghiệp vụ và nguồn sự thật duy nhất cho toàn bộ dữ liệu do Emotion Service sở hữu.
- Bằng chứng: POM có JPA/Flyway/PostgreSQL; V1 tạo đủ 7 bảng trong PostgreSQL; tài liệu tổng quan mô tả PostgreSQL tập trung.
- Phạm vi: journal, health metrics, assessment catalog/result, risk alert và các bảng bổ sung trong tương lai đều lưu bằng PostgreSQL/Flyway.
- Không áp dụng: dual-write và database engine nghiệp vụ thứ hai.
- Hệ quả: tối ưu time-series bằng thiết kế bảng, partitioning/index/materialized view của PostgreSQL khi workload yêu cầu. Redis chỉ có thể là cache/dữ liệu tạm thời, không phải nguồn sự thật.

## DEC-003 - REST đồng bộ, RabbitMQ cho side effect bất đồng bộ

- Trạng thái: **Provisional**
- Ngày ghi nhận: 2026-07-22
- Quyết định: REST trả kết quả cho CRUD/query/submit; event dùng cho notification, phân tích nền và integration lỏng.
- Hệ quả: delivery at-least-once, cần outbox/inbox, idempotency, retry/DLQ và schema version.
- Hiện trạng: RabbitMQ chưa có dependency/config/code trong repository.

## DEC-004 - API v1, identity và role từ security context

- Trạng thái: **Accepted** cho user identity và role; service identity còn provisional
- Ngày ghi nhận: 2026-07-22; cập nhật theo chỉ thị chủ dự án ngày 2026-07-24
- Quyết định: public REST dưới `/api/v1`; Gateway xác thực JWT và truyền UUID qua verified header `X-User-Id`, claim role đơn qua `X-User-Role`. Role hợp lệ là `ROLE_USER`, `ROLE_EXPERT`, `ROLE_ADMIN`. `OncePerRequestFilter` tạo `AuthenticatedUser`, authority tương ứng và nạp authentication stateless vào `SecurityContextHolder`. Controller dùng `@AuthenticationPrincipal`, không nhận `userId` do client chọn; `/api/v1/admin/**` yêu cầu `ROLE_ADMIN`, `/api/v1/assessments/**` và `/api/v1/assessment-results/**` yêu cầu `ROLE_USER`.
- Lý do: giảm IDOR và tạo contract version rõ.
- Hệ quả bảo mật: Gateway phải xóa/ghi đè cả `X-User-Id` và `X-User-Role` từ client; Emotion Service không được expose trực tiếp và network policy phải chỉ cho traffic tin cậy. Header không tự chứng minh tính xác thực nếu bỏ qua các điều kiện triển khai này.
- Gateway hiện chưa có code chuyển tiếp claim; cần triển khai trước production. Service-to-service identity và cơ chế bảo vệ hop Gateway→service vẫn chưa được chốt.

## DEC-005 - Assessment và risk rule phải version hóa

- Trạng thái: **Provisional**
- Ngày ghi nhận: 2026-07-22
- Quyết định: assessment sau publish là immutable; thay đổi tạo version. Result/alert lưu version đủ để giải thích và tái tạo.
- Lý do: thang điểm/ngưỡng có ý nghĩa chuyên môn; thay đổi hồi tố sẽ phá lịch sử và an toàn.
- Hệ quả: schema V1 chưa đủ, cần migration trước capability assessment production.

## DEC-006 - Cảnh báo là hỗ trợ, không phải chẩn đoán

- Trạng thái: **Provisional**
- Ngày ghi nhận: 2026-07-22
- Quyết định: response/cảnh báo dùng ngôn ngữ sàng lọc, lý do có cấu trúc và hành động an toàn. Không phát chẩn đoán tự động.
- Hệ quả: clinical review, disclaimer, crisis escalation và safety test là gate production.

## DEC-007 - UUID, audit fields và soft delete

- Trạng thái: **Observed** cho V1; policy chi tiết còn mở
- Ngày ghi nhận: 2026-07-22
- Quyết định hiện tại: UUID PK; mọi bảng V1 có `created_at`, `updated_at`, `deleted_at`.
- Khoảng trống: `updated_at` chưa tự cập nhật; retention/hard delete/restore/audit actor chưa xác định; timestamp chưa có timezone.

## DEC-008 - Eureka là tùy chọn ở local

- Trạng thái: **Observed**
- Ngày ghi nhận: 2026-07-22
- Hiện trạng: dependency Eureka client có trong POM nhưng `eureka.client.enabled: false` trong `application.yml`.
- Hệ quả: không viết code nghiệp vụ phụ thuộc service discovery; production topology cần platform decision.

## DEC-009 - UUID và thời gian do application quản lý

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-22
- Người duyệt: chủ dự án, qua chỉ thị trực tiếp cho Emotion Service.
- Quyết định: toàn bộ trường thời gian trong PostgreSQL dùng `TIMESTAMPTZ`; entity dùng `OffsetDateTime`.
- Quyết định: UUID sinh ở application bằng Hibernate `GenerationType.UUID`; migration không khai báo `DEFAULT uuid_generate_v4()`.
- Quyết định: `created_at` và `updated_at` dùng Spring Data JPA Auditing; không dùng PostgreSQL trigger hoặc database default.
- Migration: giữ nguyên V1 và dùng V2 để bỏ default, chuyển dữ liệu `TIMESTAMP` cũ với giả định các giá trị đó đang biểu diễn UTC.

## DEC-010 - Baseline business rule cho Service Implementation

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-22
- Người duyệt: chủ dự án, ủy quyền trực tiếp cho coding agent chọn phương án tối ưu để hoàn thành phase.
- Quyết định: lịch sử dùng keyset cursor, khoảng `[from, to)`, limit tối đa 100; journal dùng mapping `emotion-v1`; health batch atomic tối đa 100 item với idempotency và canonical unit.
- Quyết định: assessment published immutable và version hóa; Phase hiện hỗ trợ scoring policy PHQ-9 v1 và GAD-7 v1. DASS-21 không được publish khi chưa có policy được phê duyệt.
- Quyết định: `risk-v1` chỉ tạo alert hỗ trợ từ assessment gần nhất: `SEVERE -> ELEVATED`, `EXTREME -> HIGH`; không tạo composite clinical score từ journal/health và không phát chẩn đoán.
- Migration: V3 bổ sung schema prerequisite cho idempotency, lifecycle/version, result traceability và alert deduplication.
- Giới hạn: crisis/critical escalation, clinical approval DASS, consent/retention và notification delivery vẫn là production gate riêng.

## DEC-011 - Journal bất biến và cửa sổ soft delete

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-24
- Người duyệt: chủ dự án, qua chỉ thị trực tiếp.
- Quyết định: emotion journal không có update use case vì phản ánh trạng thái tại đúng thời điểm ghi nhận.
- Quyết định: owner chỉ được soft delete từ lúc tạo đến hết đúng phút thứ 15, tức `now <= createdAt + 15 phút`. Sau thời điểm này trả `JOURNAL_DELETION_WINDOW_EXPIRED`.
- Quyết định: chưa áp dụng rate limit create trong giai đoạn test; trước production phải bổ sung policy chống spam có cấu hình và test.
- Hệ quả: loại bỏ DTO/service contract update journal; API chỉ expose create/read/history/delete. Mọi read mặc định loại `deleted_at`.
- Migration: không cần migration mới vì schema hiện tại đã có `created_at` và `deleted_at`.

## DEC-012 - Assessment code là tập đóng ở application

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-24
- Người duyệt: chủ dự án, qua chỉ thị trực tiếp.
- Quyết định: phạm vi hiện tại chỉ gồm `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`. Java dùng `AssessmentCode` xuyên suốt entity/repository/service/DTO; JSON, URL và database dùng code canonical có dấu gạch ngang.
- Quyết định: PostgreSQL tiếp tục lưu `VARCHAR(50)`, không tạo native enum hoặc check constraint. JPA `AttributeConverter` bảo toàn giá trị canonical, giúp bổ sung loại mới sau này không bắt buộc thay database type.
- Quyết định: code bất biến giữa các version. Entity không khai báo unique riêng trên `code`; schema V3 dùng unique `(code, assessment_version)` và partial unique cho một published version.
- Giới hạn: enum chỉ giới hạn catalog, không xác nhận scoring policy. PHQ-9/GAD-7 đang có policy; DASS-21/PSS-10/WHO-5 chưa được publish trước khi có rule version và test biên được duyệt.
- Migration: không cần migration mới vì kiểu và giá trị cột không thay đổi.

## DEC-013 - Assessment draft là aggregate được tạo atomically

- Trạng thái: **Superseded** bởi DEC-015
- Ngày ghi nhận: 2026-07-24
- Người duyệt: chủ dự án, qua ủy quyền chọn phương án tối ưu khi triển khai UC Admin create draft.
- Quyết định: POST create lưu assessment version 1 ở `DRAFT`, toàn bộ question và answer option trong một transaction. Question dùng `orderIndex` không âm/không trùng; option dùng thứ tự mảng request làm `orderIndex`.
- Quyết định lịch sử: admin từng gửi options và draft chấp nhận `scoreValue` chung từ 0 đến 5. DEC-015 thay thế phần này bằng server-generated response scale theo code.
- Quyết định: create từ chối code đã tồn tại; version mới phải đi qua update/versioning use case để bảo toàn lịch sử.
- Hệ quả: response `201 Created` trả aggregate đã persist và Location resource. Lỗi nested validation xảy ra trước service; lỗi persistence làm rollback toàn bộ.
- Migration: không cần migration mới; V3 đã có lifecycle/version và constraint ordering cần thiết.

## DEC-014 - Update assessment draft là full aggregate replacement

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-24
- Người duyệt: chủ dự án, qua ủy quyền chọn phương án tối ưu khi triển khai UC Admin update draft.
- Quyết định: `PUT /api/v1/admin/assessments/{id}` chỉ cập nhật assessment ở `DRAFT`; `id`, `code`, `assessmentVersion` và `status` bất biến.
- Quyết định: request thay thế toàn bộ question catalog. Question/option active cũ được soft-delete, questions mới và options do server sinh được persist trong cùng transaction và dùng lại invariant của create draft.
- Quyết định: PUT lên `PUBLISHED`/`ARCHIVED` bị từ chối; không ngầm tạo version mới. Tạo draft version tiếp theo phải là UC versioning rõ ràng.
- Lý do: request hiện không mang ID children, nên full replacement có semantics rõ ràng, tránh merge mơ hồ và vẫn giữ dấu vết soft-delete. Tách versioning giúp PUT không có side effect bất ngờ.
- Migration: không cần migration mới; V3 đã có soft-delete và partial unique index theo active ordering.

## DEC-015 - Response scale do server quản lý theo AssessmentCode

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-24
- Người duyệt: chủ dự án, qua chỉ thị tinh chỉnh assessment draft và ủy quyền chọn phương án tối ưu.
- Quyết định: `AssessmentDefinitionRegistry` là nguồn cấu trúc cho năm code: PHQ-9 `9×(0..3)`, GAD-7 `7×(0..3)`, DASS-21 `21×(0..3)`, PSS-10 `10×(0..4)`, WHO-5 `5×(0..5)`.
- Quyết định: create/update request chỉ nhận questions; answer options do server sinh theo definition và vẫn được lưu PostgreSQL làm snapshot theo assessment version. Request gửi `answerOptions` bị từ chối, không bị im lặng bỏ qua.
- Quyết định: draft cần từ 1 đến N câu và thứ tự liên tục `0..n-1`; publish cần đúng N câu, scale khớp definition và scoring policy đã được phê duyệt.
- Quyết định: `answer_options.score_value` là raw value. Việc đảo điểm PSS, gom subscale DASS và chuyển đổi WHO-5 phải nằm trong scoring policy versioned; không suy diễn từ raw value chung.
- Lý do: loại lỗi nhập option/điểm thủ công, giữ API admin gọn, tập trung invariant và vẫn bảo toàn khả năng truy vết dữ liệu đã publish.
- Tương thích: đây là thay đổi breaking của request create/update trong `/api/v1` khi API chưa phát hành production. Draft cũ có scale thủ công cần PUT lại một lần để được normalize trước publish; published version không bị sửa.
- Clinical gate: nhãn tiếng Việt và các scoring policy ngoài PHQ-9/GAD-7 vẫn cần chuyên gia duyệt trước production.
- Migration: không cần migration mới vì schema hiện tại đã lưu được options được materialize.

## DEC-016 - Lifecycle publish/archive của assessment catalog

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-25
- Người duyệt: chủ dự án, qua chỉ thị trực tiếp triển khai UC Admin publish và archive.
- Quyết định: action API dùng `POST /api/v1/admin/assessments/{id}:publish` và `POST /api/v1/admin/assessments/{id}:archive`, chỉ dành cho `ROLE_ADMIN` và không nhận request body.
- Quyết định: publish chỉ nhận `DRAFT` đã vượt qua đầy đủ definition/scoring-policy gate. Trong transaction publish, version published hiện tại của cùng code được archive trước khi version mới được publish, phù hợp partial unique index của V3.
- Quyết định: archive chỉ nhận `PUBLISHED`; không cho `DRAFT -> ARCHIVED` hoặc archive lặp lại. Archive là chuyển lifecycle, không soft-delete catalog và không ảnh hưởng result lịch sử.
- Lý do: lifecycle một chiều làm trạng thái dễ kiểm soát, không cho bỏ qua clinical publish gate và bảo toàn khả năng truy vết dữ liệu đã sử dụng.
- Migration: không cần migration mới; V3 đã có status, version và partial unique index cho một published version mỗi code.

## DEC-017 - Tạo draft version kế tiếp bằng clone

- Trạng thái: **Accepted**
- Ngày ghi nhận: 2026-07-25
- Người duyệt: chủ dự án, qua chỉ thị trực tiếp triển khai UC tạo draft cho version mới của assessment đã publish.
- Quyết định: action API là `POST /api/v1/admin/assessments/{id}:create-next-version`, chỉ dành cho `ROLE_ADMIN`, không nhận body và chỉ chấp nhận source `PUBLISHED`.
- Quyết định: version mới là `max(assessment_version) + 1` của cùng code, có ID mới và status `DRAFT`. Title/description/questions được clone; options được materialize lại từ `AssessmentDefinitionRegistry` hiện hành thay vì dùng chung hoặc sửa snapshot cũ.
- Quyết định: mỗi code chỉ có một draft chờ. Service khóa pessimistic version mới nhất trong transaction; unique `(code, assessment_version)` ánh xạ race còn lại thành `ASSESSMENT_VERSION_CONFLICT`.
- Lý do: clone cho admin một điểm bắt đầu an toàn, giữ published aggregate/result immutable và tránh nhập lại toàn bộ nội dung; option mới vẫn tuân theo definition tập trung.
- Hệ quả: cả service lock/invariant và database constraint cùng bảo vệ thao tác; writer khác không thể tạo draft active thứ hai cho cùng code.
- Migration: V4 thêm partial unique index `uq_assessment_one_draft_version` trên `assessments(code)` khi `status = 'DRAFT' AND deleted_at IS NULL`; không sửa V1-V3.

## Quyết định mở cần ưu tiên

| ID | Câu hỏi | Người/nhóm cần tham gia | Chặn |
|---|---|---|---|
| OPEN-02 | Service-to-service auth và bảo vệ hop Gateway→service là gì? | Auth, Gateway, security | API nội bộ/expert và production security |
| OPEN-03 | Consent chia sẻ expert được lưu/kiểm tra ở đâu? | Product, Booking, privacy | UC 5.6 |
| OPEN-04 | Threshold/nội dung DASS-21 và policy assessment mới nào được phê duyệt? | Clinical expert, product | assessment ngoài PHQ-9/GAD-7 |
| OPEN-05 | Crisis escalation và trách nhiệm notification? | Clinical, legal, product, mobile | high-risk alert |
| OPEN-06 | Retention, erasure, residency, encryption key model? | Privacy, security, platform | production |
| OPEN-07 | SLO/RPO/RTO và workload mục tiêu? | Product, SRE/backend | capacity/operations |

## Mẫu ghi quyết định mới

```text
## DEC-NNN - Tên quyết định
- Trạng thái: Provisional | Accepted | Superseded
- Ngày / người duyệt / liên kết:
- Bối cảnh:
- Các lựa chọn:
- Quyết định và lý do:
- Hệ quả/rủi ro:
- Kế hoạch migration/rollback:
```
