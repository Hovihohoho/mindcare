# Cơ sở dữ liệu Emotion Service

## Baseline hiện tại

- PostgreSQL, schema `emotion_schema`.
- Flyway quản lý migration V1-V4 tại `src/main/resources/db/migration`.
- JPA dùng `ddl-auto: validate`; schema phải do Flyway tạo.
- V1 từng khai báo default `uuid_generate_v4()`; V2 bỏ toàn bộ UUID default. Entity sinh UUID bằng Hibernate `GenerationType.UUID`.
- V2 chuyển toàn bộ trường thời gian hiện có sang `TIMESTAMPTZ`. Entity dùng `OffsetDateTime`.
- `created_at` và `updated_at` không có database default/trigger; Spring Data JPA Auditing chịu trách nhiệm gán giá trị.
- Không có foreign key tới bảng user/expert ở service khác; `user_id` là external reference.

PostgreSQL là cơ sở dữ liệu nghiệp vụ duy nhất của Emotion Service. Không triển khai dual-write hoặc một nguồn dữ liệu nghiệp vụ thứ hai. Redis, nếu được bổ sung, chỉ là cache/dữ liệu tạm thời và luôn có thể tái tạo từ PostgreSQL.

## Thay đổi V3

- `health_metrics.external_sample_id` và unique index theo user/source để deduplicate sample.
- Bảng `health_metric_sync_requests` lưu request hash và response JSONB cho idempotency.
- `assessments` có `assessment_version`, `status` và ràng buộc chỉ một published version cho mỗi code.
- `answer_options.order_index` và unique partial index cho thứ tự question/option đang hoạt động.
- `assessment_results` lưu assessment/scoring version, idempotency, submission hash, notice và recommendations.
- `psychological_alert_logs` lưu rule/reason/dedup/source result/notified timestamp.
- Thêm partial/composite index cho active history và alert deduplication.

## Mô hình hiện tại sau V3

```text
assessments 1 --- n questions 1 --- n answer_options
      |
      +--- n assessment_results

emotion_journals          -- user_id external
health_metrics            -- user_id external
health_metric_sync_requests -- idempotency metadata
assessment_results        -- user_id external
psychological_alert_logs  -- user_id external
```

### Bảng và cột

#### `emotion_journals`

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | UUID | PK, sinh ở application bằng `GenerationType.UUID` |
| `user_id` | UUID | bắt buộc, tham chiếu logic Auth Service |
| `emotion_type` | varchar(50) | bắt buộc; chưa có DB check constraint |
| `content` | text | tùy chọn |
| `created_at`, `updated_at` | timestamptz | bắt buộc, do JPA Auditing quản lý |
| `deleted_at` | timestamptz | soft delete, nullable |

Index: `(user_id, created_at)`.

#### `health_metrics`

| Cột | Kiểu | Ràng buộc/ý nghĩa |
|---|---|---|
| `id` | UUID | PK |
| `user_id` | UUID | external reference |
| `metric_type` | varchar(50) | ví dụ sleep/heart rate/steps |
| `metric_value` | numeric(10,2) | bắt buộc |
| `unit` | varchar(20) | nullable trong V1 |
| `source_type` | varchar(50) | Apple/Google/manual |
| `external_sample_id` | varchar(255) | ID nguồn, unique theo user/source khi bản ghi active |
| `recorded_at` | timestamptz | thời điểm đo, ánh xạ `OffsetDateTime` |
| audit fields | timestamptz | create/update/delete; create/update do JPA Auditing quản lý |

Index: `(user_id, recorded_at)`.

`health_metric_sync_requests` lưu `(user_id, source_type, idempotency_key)` unique, request hash và response JSONB; bảng này là metadata idempotency, không chứa metric detail.

#### `assessments`

`id`, `code`, `assessment_version`, `status`, `title`, `description` và audit fields. `code` tiếp tục là `VARCHAR(50)` với giá trị canonical `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`; giới hạn tập code được thực thi bằng enum/converter ở application, không dùng PostgreSQL native enum. Unique `(code, assessment_version)`; partial unique index đảm bảo tối đa một version `PUBLISHED` đang hoạt động cho mỗi code.

Khi tạo draft version tiếp theo, application khóa pessimistic bản active có version lớn nhất của cùng code, kiểm tra không có draft chờ rồi insert version `max + 1`. Unique `(code, assessment_version)` vẫn bảo vệ race condition về số version; V4 bổ sung partial unique index `uq_assessment_one_draft_version` để đảm bảo tối đa một `DRAFT` active cho mỗi code ngay tại PostgreSQL.

#### `questions`

`assessment_id` FK tới `assessments` với `ON DELETE CASCADE`, `question_text`, `order_index`, audit fields. V3 đảm bảo unique order cho question đang hoạt động.

#### `answer_options`

`question_id` FK tới `questions` với `ON DELETE CASCADE`, `option_text`, `score_value`, `order_index`, audit fields. V3 đảm bảo unique order cho option đang hoạt động. Admin không ghi options trực tiếp: application materialize response scale từ `AssessmentDefinitionRegistry` cho từng question và lưu thành snapshot theo assessment version. `score_value` là raw value; scoring policy chịu trách nhiệm đảo điểm/gom subscale/chuyển đổi theo code.

#### `assessment_results`

`user_id`, `assessment_id` FK, assessment/scoring version, idempotency/submission hash, `total_score`, `risk_level`, `answers_detail JSONB`, screening notice, recommendations JSONB và audit fields. History và idempotency đều có index.

#### `psychological_alert_logs`

`user_id`, `alert_level`, `trigger_reason`, `reason_code`, `rule_version`, `source_result_id`, `deduplication_key`, `is_notified`, `notified_at` và audit fields. Confidence/data window/delivery ID vẫn để cho migration sau khi contract notification được duyệt.

## Khoảng trống so với yêu cầu

| Nhu cầu | V1 | Hướng thay đổi dự kiến |
|---|---|---|
| Assessment version/publish | V3 có version/status | `published_at` chỉ bổ sung nếu reporting yêu cầu |
| Risk thresholds | Thiếu bảng | thêm threshold theo assessment version, miền điểm không chồng lấn |
| Idempotent health sync | V3 đã có | integration test atomic/idempotency đã chạy xanh trên PostgreSQL; còn thiếu concurrency test |
| Consent/sync state | Thiếu | xác định ownership trước; chỉ thêm nếu Emotion Service sở hữu |
| Reproducible result | V3 có snapshot/version/rule | cân nhắc recommendation template version riêng khi content service hóa |
| Explainable alert | V3 có reason/rule/source/dedup | bổ sung confidence/window/delivery ID khi contract được duyệt |
| Enum/check constraint | varchar tự do | DB check hoặc lookup/versioned catalog phù hợp |
| Soft-delete uniqueness | Chưa xét | partial unique index nếu nghiệp vụ yêu cầu |

## Quy tắc schema

1. Tên bảng/cột `snake_case`, số nhiều cho bảng.
2. UUID PK được sinh ở application bằng `GenerationType.UUID`; database không khai báo UUID default. External service ID không có FK xuyên service.
3. Monetary value không thuộc schema này. Metric dùng decimal, nhưng step count cần validation số nguyên.
4. Mọi trường thời gian dùng `TIMESTAMPTZ`/`OffsetDateTime`; thời gian nghiệp vụ bắt buộc phân biệt với audit time.
5. JSONB chỉ dùng cho snapshot/thuộc tính biến đổi có lý do; field cần lọc/join/report phải chuẩn hóa thành cột/bảng.
6. Index phải xuất phát từ query pattern và kiểm tra bằng `EXPLAIN`; tránh index mọi cột.
7. `deleted_at IS NULL` là điều kiện mặc định; index có thể dùng partial predicate khi workload xác nhận.
8. Mọi capability mới của service phải mở rộng PostgreSQL/Flyway; không đưa thêm database engine vào kiến trúc.
9. `created_at`/`updated_at` do Spring Data JPA Auditing quản lý; không dùng PostgreSQL trigger hoặc database default.

## Migration

- Naming: `V{n}__Mo_ta_ngan.sql`, không đổi file/version đã áp dụng ở môi trường chung.
- Migration cần chạy được trên database rỗng và upgrade từ version trước.
- Thay đổi phá vỡ dùng expand/migrate/contract: thêm cấu trúc tương thích, backfill, chuyển code, rồi xóa ở release sau.
- DDL và data backfill lớn tách riêng; đánh giá lock/time trước production.
- Không dùng Hibernate tự tạo/sửa schema.
- `CREATE EXTENSION` có thể cần quyền cao; production provisioning phải quyết định extension được tạo bởi platform hay Flyway.

### PostgreSQL integration test

- Testcontainers dùng image cố định `postgres:16.8-alpine` và Spring Boot `@ServiceConnection`.
- Migration test được cấu hình chạy V1-V4 trên database rỗng, kiểm tra schema/type/default/constraint/index và một luồng upgrade có dữ liệu từ V1 sang V4. Lượt gần nhất có Docker mới chỉ xác minh đến V3; V4 đang chờ chạy lại Testcontainers.
- Repository test kiểm tra ownership, soft-delete, keyset query và assessment lifecycle trên PostgreSQL.
- Health service test kiểm tra canonical unit, transaction atomic và idempotency bằng persistence thật.
- Dùng `@Testcontainers(disabledWithoutDocker = true)`: thiếu Docker làm test bị skip có báo cáo, không được diễn giải là PostgreSQL test đã xanh.
- Lần chạy `mvn.cmd verify` ngày 2026-07-23 đã thực thi đủ 20/20 test trên PostgreSQL 16.8, gồm cả migration V1→V3 và keyset query với/không có cursor.

## Backup, retention và quyền DB

Chưa có SLO chính thức cho backup/RPO/RTO/retention. Trước production cần:

- role runtime chỉ có DML cần thiết; role migration tách riêng có DDL;
- backup mã hóa và restore drill;
- policy retention/hard delete cho journal, metric, result, alert;
- audit truy cập của expert/admin và quy trình đáp ứng yêu cầu export/xóa dữ liệu;
- không dùng credential mặc định trong `application.yml` ở môi trường chia sẻ.
