# REST API Contract mục tiêu

> Trạng thái: thiết kế baseline. Create, detail, history, soft delete và emotion trend đã được triển khai; các operation còn lại vẫn là contract mục tiêu. OpenAPI chưa được bổ sung.

## Quy ước chung

- Base path: `/api/v1`.
- JSON UTF-8; field `camelCase`; enum `UPPER_SNAKE_CASE`.
- Timestamp ISO-8601 có offset, response chuẩn hóa UTC (`Z`).
- Gateway xác thực JWT, xóa/ghi đè giá trị client và truyền UUID qua `X-User-Id`, role đơn qua `X-User-Role`; Emotion Service nạp identity và authority vào SecurityContext. `X-User-Role` chỉ nhận `ROLE_USER`, `ROLE_EXPERT`, `ROLE_ADMIN`. Endpoint self-service không nhận `userId` trong body/query.
- Controller lấy `AuthenticatedUser` bằng `@AuthenticationPrincipal` rồi truyền `userId` rõ ràng vào service. Không đọc header trực tiếp trong từng controller.
- `X-User-Id` và `X-User-Role` chỉ an toàn khi service không bị expose trực tiếp và Gateway/network policy bảo đảm header là verified. Header role không hợp lệ làm request bị từ chối; namespace `/api/v1/admin/**` yêu cầu authority `ROLE_ADMIN`.
- `X-Correlation-Id` được tiếp nhận/sinh mới và trả lại.
- `Idempotency-Key` bắt buộc cho submit assessment và batch health sync; nên hỗ trợ khi tạo journal từ client offline.
- Pagination cursor được ưu tiên cho chuỗi thời gian; nếu dùng page/size phải có sort ổn định `createdAt,id`.

## Response và lỗi

Success trả resource trực tiếp hoặc envelope phân trang:

```json
{
  "items": [],
  "nextCursor": null,
  "hasMore": false
}
```

Error theo Problem Details-compatible shape:

```json
{
  "type": "https://api.mindcare.vn/problems/validation-error",
  "title": "Request validation failed",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "One or more fields are invalid",
  "traceId": "01J...",
  "fieldErrors": [{"field": "emotionType", "code": "INVALID_VALUE"}]
}
```

Request DTO dùng Jakarta Validation cho shape và constraint từng field; collection lồng nhau phải dùng `@Valid`. Các invariant phụ thuộc nhiều field, catalog hoặc trạng thái dữ liệu vẫn do application service kiểm tra. Giới hạn nội dung nhật ký được tính theo Unicode code point, không theo số UTF-16 code unit.

Status chính:

- `400 VALIDATION_ERROR`: field/parameter không hợp lệ; `fieldErrors` chứa đường dẫn field và mã constraint ổn định.
- `400 MALFORMED_REQUEST`: JSON sai cú pháp, thiếu header/parameter bắt buộc hoặc không chuyển đổi được kiểu.
- `401 UNAUTHORIZED`: thiếu/sai xác thực.
- `403 FORBIDDEN`: không có quyền/consent.
- `404 RESOURCE_NOT_FOUND`: resource không tồn tại trong phạm vi nhìn thấy hoặc route/static resource không tồn tại.
- `405 METHOD_NOT_ALLOWED`: HTTP method không được hỗ trợ; response có header `Allow` khi xác định được.
- `406 NOT_ACCEPTABLE`: server không thể tạo representation theo `Accept`.
- `409 CONFLICT`: conflict hoặc idempotency mismatch.
- `415 UNSUPPORTED_MEDIA_TYPE`: `Content-Type` không được hỗ trợ.
- `422 BUSINESS_RULE_VIOLATION`: vi phạm rule nghiệp vụ không thuộc validation shape.
- `429 TOO_MANY_REQUESTS`: rate limited.
- `500 INTERNAL_SERVER_ERROR`: lỗi không dự kiến.

Không trả stack trace, exception message nội bộ hoặc chi tiết DB.

## Emotion journals

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| POST | `/emotion-journals` | Authenticated user | Tạo nhật ký của chính user — đã triển khai |
| GET | `/emotion-journals` | Authenticated user | Lịch sử theo `from`, `to`, `cursor`, `limit` — đã triển khai |
| GET | `/emotion-journals/{id}` | Authenticated user | Xem chi tiết thuộc user — đã triển khai |
| DELETE | `/emotion-journals/{id}` | Authenticated user | Xóa mềm trong 15 phút sau khi tạo — đã triển khai |
| GET | `/emotion-trends` | Authenticated user | Time-series với `from`, `to`, `bucket`, `timezone` — đã triển khai |

Create request:

```json
{
  "emotionType": "SAD",
  "content": "Hôm nay mình thấy quá tải."
}
```

Response thành công: `201 Created`, body là resource bên dưới và header `Location: /api/v1/emotion-journals/{id}`.

```json
{
  "id": "0d820498-7af0-47bf-ae6f-fcf704e66416",
  "emotionType": "SAD",
  "content": "Hôm nay mình thấy quá tải.",
  "createdAt": "2026-07-22T08:00:00Z",
  "updatedAt": "2026-07-22T08:00:00Z"
}
```

Detail thành công trả `200 OK` với cùng response schema. Truy vấn bắt buộc đồng thời khớp `id`, `userId` từ SecurityContext và `deletedAt IS NULL`; journal không tồn tại, đã xóa hoặc thuộc user khác đều trả cùng `404 RESOURCE_NOT_FOUND`.

History request:

```http
GET /api/v1/emotion-journals
    ?from=2026-10-01T00:00:00%2B07:00
    &to=2026-11-01T00:00:00%2B07:00
    &limit=20
    &cursor=<opaque-cursor>
```

- Khoảng thời gian là `[from, to)`, tối đa 365 ngày. `from` và `to` dùng ISO-8601 có offset; dấu `+` trong query string phải được URL encode thành `%2B`.
- `limit` mặc định `20`, hợp lệ từ `1` đến `100`.
- `cursor` không truyền ở trang đầu. Trang sau phải gửi nguyên `nextCursor` cùng chính `from`, `to` và user; cursor là opaque, client không tự giải mã hoặc sửa.
- Kết quả sắp xếp mới nhất trước theo `createdAt DESC, id DESC`, chỉ chứa journal của user hiện tại và tự động loại bản ghi soft-delete.
- Frontend chọn tuần/tháng theo timezone hiển thị rồi gửi mốc đầu kỳ và đầu kỳ kế tiếp với offset tương ứng. Ví dụ tháng 10 là `[01/10 00:00, 01/11 00:00)`.

```json
{
  "items": [
    {
      "id": "0d820498-7af0-47bf-ae6f-fcf704e66416",
      "emotionType": "SAD",
      "content": "Hôm nay mình thấy quá tải.",
      "createdAt": "2026-10-07T08:00:00Z",
      "updatedAt": "2026-10-07T08:00:00Z"
    }
  ],
  "nextCursor": "opaque-value-or-null",
  "hasMore": true
}
```

API history cung cấp dữ liệu thô cho danh sách và lịch tháng. Biểu đồ thống kê tuần/tháng không tính ở controller hoặc frontend từ một page chưa đầy đủ; dùng endpoint trend/aggregation thuộc UC riêng.

Delete thành công trả `204 No Content`. Server lấy thời gian hiện tại từ application clock và cho phép xóa đến hết đúng thời điểm `createdAt + 15 phút`. Quá cửa sổ trả:

```json
{
  "status": 422,
  "code": "JOURNAL_DELETION_WINDOW_EXPIRED",
  "detail": "Emotion journal can only be deleted within 15 minutes after creation"
}
```

Journal không tồn tại, đã xóa hoặc thuộc user khác đều trả cùng `404 RESOURCE_NOT_FOUND`. Không có endpoint update journal vì bản ghi phản ánh trạng thái tại thời điểm ghi nhận. Trong giai đoạn test chưa áp dụng rate limit cho create; đây là production prerequisite chống spam.

Trend request:

```http
GET /api/v1/emotion-trends
    ?from=2026-07-20T00:00:00%2B07:00
    &to=2026-07-27T00:00:00%2B07:00
    &bucket=DAY
    &timezone=Asia%2FHo_Chi_Minh
```

- Khoảng thời gian là `[from, to)`, tối đa 730 ngày. `from` và `to` là ISO-8601 có offset; `timezone` là IANA Zone ID hợp lệ.
- `bucket` nhận `DAY`, `WEEK` hoặc `MONTH`; tuần bắt đầu thứ Hai, tháng bắt đầu ngày 1 trong timezone yêu cầu.
- Frontend nên gửi mốc đã căn theo kỳ: biểu đồ tuần dùng 7 ngày và `DAY`; lịch/biểu đồ trong một tháng dùng đầu tháng đến đầu tháng kế tiếp và `DAY`; so sánh nhiều tháng dùng `MONTH`.
- Chỉ tổng hợp journal chưa bị soft-delete của user hiện tại. Response là mảng các bucket liên tục; bucket không có dữ liệu trả `averageScore: null`, `count: 0`.
- Mapping `emotion-v1`: `VERY_HAPPY=2`, `HAPPY=1`, `NEUTRAL=0`, `SAD=-1`, `STRESSED=-2`. Điểm trung bình là tổng điểm chia số journal và làm tròn 2 chữ số.

```json
[
  {
    "periodStart": "2026-07-20T00:00:00+07:00",
    "periodEnd": "2026-07-21T00:00:00+07:00",
    "averageScore": 1.00,
    "count": 2,
    "mappingVersion": "emotion-v1"
  },
  {
    "periodStart": "2026-07-21T00:00:00+07:00",
    "periodEnd": "2026-07-22T00:00:00+07:00",
    "averageScore": null,
    "count": 0,
    "mappingVersion": "emotion-v1"
  }
]
```

Request thiếu/sai timestamp trả `400 MALFORMED_REQUEST`; timezone không hợp lệ trả `400 INVALID_TIMEZONE`; bucket ngoài danh sách trả `400 INVALID_BUCKET`.

## Health metrics

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| POST | `/api/v1/health-metrics/sync` | USER | Nhận batch upsert idempotent (tối đa 100) |
| GET | `/health-metrics` | USER | Truy vấn theo type/time range |
| GET | `/api/v1/health-metrics/trends` | USER | Aggregate theo type/bucket/timezone |
| GET | `/api/v1/health-metrics/stress-features` | USER | Tạo vector 25 feature PMData v2 từ dữ liệu sức khỏe của chính user |
| GET | `/api/v1/health-metrics/wellness-features` | USER | Tạo vector LifeSnaps 42 feature theo đúng thứ tự model |
| GET | `/api/v1/health-metrics/daily-evaluation` | USER | Đánh giá ngày hoàn tất gần nhất; benchmark khoa học là kết luận chính, baseline cá nhân 28 ngày là bổ sung |

Stress feature request:

```http
GET /api/v1/health-metrics/stress-features
    ?date=2026-09-09
    &timezone=Asia%2FHo_Chi_Minh
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_USER
```

- `date` là ngày feature hiện tại; bỏ trống thì server dùng ngày hoàn tất gần nhất (hôm qua theo timezone).
- Server chỉ đọc dữ liệu của user trong SecurityContext, dùng ngày hiện tại và tối đa 7 ngày lịch sử.
- Thứ tự 25 giá trị cố định theo `pmdata-features-v2`; giá trị không có dữ liệu trả `null`, không tự coi là bình thường.
- Heart rate dùng mean, population standard deviation và sample count; resting heart rate chỉ dùng sample có `measurementContext=RESTING`.
- Sleep chọn session dài nhất trong ngày. Khi có stage, server tính asleep/deep/REM/awake và efficiency; khi không có stage chỉ dùng duration.
- Trend 3/7 ngày chỉ dùng các ngày trước `date`, cùng quy tắc minimum 2/3 observation như pipeline train.
- Response chứa `featureVersion`, `featureDate`, `timezone`, `featureNames`, `features` và `availableBaseFeatureCount`.

```json
{
  "sourceType": "HEALTH_CONNECT",
  "items": [
    {
      "externalSampleId": "sleep:source-id",
      "metricType": "SLEEP_SESSION",
      "recordedAt": "2026-07-21T23:00:00+07:00",
      "startTime": "2026-07-21T16:30:00+07:00",
      "endTime": "2026-07-21T23:00:00+07:00",
      "dataOrigin": "com.example.wearable",
      "sourceLastModifiedAt": "2026-07-21T23:05:00+07:00"
    }
  ]
}
```

Response trả `acceptedCount`, `updatedCount`, `duplicateCount` và ID đã xử lý. Không echo dữ liệu sức khỏe.

## Assessments cho user

`assessmentCode` nhận một trong `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`. API luôn dùng chuỗi canonical có dấu gạch ngang; tên enum Java nội bộ như `PHQ_9` không xuất hiện trong JSON hoặc URL.

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| GET | `/assessments` | USER | Danh sách phiên bản đang publish — đã triển khai |
| GET | `/assessments/{code}` | USER | Nội dung bài hiện hành, không trả score — đã triển khai |
| POST | `/assessments/{code}/submissions` | USER | Validate, chấm điểm và lưu atomically — đã triển khai |
| GET | `/assessment-results` | USER | Lịch sử kết quả của user — đã triển khai |
| GET | `/assessment-results/{id}` | USER | Chi tiết kết quả thuộc user — đã triển khai |

Published assessment list:

```http
GET /api/v1/assessments
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_USER
```

- Thành công trả `200 OK` và JSON array sắp xếp theo code tăng dần. Không có catalog published trả `[]`.
- Mỗi item gồm `id`, `code`, `assessmentVersion`, `title`, `description`; không chứa questions/options/score/status.
- Chỉ trả assessment `PUBLISHED` chưa soft-delete. `ROLE_ADMIN`/`ROLE_EXPERT` không dùng endpoint này nếu không có `ROLE_USER`.
- Thiếu identity trả `401 AUTHENTICATION_REQUIRED`; sai role trả `403 ACCESS_DENIED`.

Published assessment detail:

```http
GET /api/v1/assessments/PHQ-9
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_USER
```

- Thành công trả `200 OK` với assessment `id`, code, version, title, description và questions/options theo `orderIndex`.
- Answer option chỉ có `id` và `optionText`; API không trả `scoreValue` hoặc status.
- Code hợp lệ nhưng chưa có version published trả `404 RESOURCE_NOT_FOUND`; draft/archived/soft-delete không được lộ.
- Code ngoài tập hỗ trợ trả `400 MALFORMED_REQUEST`; thiếu identity/sai role trả `401/403`.

Submit request không gửi điểm:

```http
POST /api/v1/assessments/PHQ-9/submissions
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_USER
Idempotency-Key: <unique-key-up-to-255-characters>
Content-Type: application/json
```

```json
{
  "assessmentVersion": 1,
  "answers": [
    {"questionId": "e1a5a724-5a5c-4c94-8a63-54fd891c76dd", "optionId": "db33eae2-468d-4677-a3c1-04866ef5468b"}
  ]
}
```

- Thành công trả `201 Created`, body là kết quả và header `Location: /api/v1/assessment-results/{resultId}`.
- Server chỉ chấm version `PUBLISHED` hiện hành, lấy `scoreValue` từ option snapshot và lưu result/answer snapshot trong cùng transaction.
- Retry cùng `Idempotency-Key` và cùng payload trả lại đúng result đã tạo; cùng key nhưng payload khác trả `409 IDEMPOTENCY_CONFLICT`.
- Version không còn hiện hành trả `409 ASSESSMENT_VERSION_CONFLICT`; thiếu/trùng/sai question hoặc option trả `400` với business error code tương ứng.
- Thiếu `Idempotency-Key` hoặc body sai định dạng/validation trả `400`; thiếu identity/sai role trả `401/403`.

Response tối thiểu:

```json
{
  "resultId": "5a36723f-b163-4f88-946e-7937374cfb0c",
  "assessmentCode": "PHQ-9",
  "assessmentVersion": 1,
  "totalScore": 8,
  "riskLevel": "MILD",
  "screeningNotice": "Kết quả này chỉ có mục đích sàng lọc, không phải chẩn đoán.",
  "recommendations": [],
  "createdAt": "2026-07-22T08:10:00Z"
}
```

Assessment result history:

```http
GET /api/v1/assessment-results?from=2026-07-01T00:00:00Z&to=2026-08-01T00:00:00Z&limit=20
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_USER
```

- Khoảng thời gian là `[from, to)`, tối đa 365 ngày; `limit` mặc định 20 và nằm trong `1..100`.
- Response là `{items, nextCursor, hasMore}`, sắp xếp theo `createdAt DESC, resultId DESC`.
- Trang sau gửi lại cùng `from`, `to`, `limit` và `cursor=<nextCursor>`. Cursor không dùng lại được cho user hoặc khoảng thời gian khác.
- Chỉ trả result active của chính user; không trả answer snapshot hoặc scoring-rule internals.

Assessment result detail:

```http
GET /api/v1/assessment-results/5a36723f-b163-4f88-946e-7937374cfb0c
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_USER
```

- Thành công trả `200 OK` với cùng schema `AssessmentResultResponse` của submit.
- Result không tồn tại, đã soft-delete hoặc thuộc user khác đều trả `404 RESOURCE_NOT_FOUND`.
- ID sai định dạng trả `400 MALFORMED_REQUEST`; thiếu identity/sai role trả `401/403`.

## Admin assessment API

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| POST | `/admin/assessments` | ADMIN | Tạo assessment draft cùng questions/options — đã triển khai |
| PUT | `/admin/assessments/{id}` | ADMIN | Thay thế toàn bộ draft — đã triển khai |
| POST | `/admin/assessments/{id}:create-next-version` | ADMIN | Clone bản published thành draft version kế tiếp — đã triển khai |
| POST | `/admin/assessments/{id}:publish` | ADMIN | Validate và publish immutable version — đã triển khai |
| POST | `/admin/assessments/{id}:archive` | ADMIN | Ngừng cung cấp cho lần làm mới — đã triển khai |
| GET | `/admin/assessments` | ADMIN | Tra cứu mọi trạng thái/version — đã triển khai |
| GET | `/admin/assessments/{id}` | ADMIN | Xem toàn bộ chi tiết một assessment version — đã triển khai |

Published assessment không update in-place. Delete catalog đã có result phải archive, không cascade làm mất lịch sử.

Admin list request:

```http
GET /api/v1/admin/assessments?status=DRAFT&limit=20&cursor=<opaque>
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_ADMIN
```

- `status` tùy chọn, nhận `DRAFT`, `PUBLISHED` hoặc `ARCHIVED`; bỏ trống để lấy mọi trạng thái.
- `limit` mặc định `20`, hợp lệ từ `1` đến `100`; `cursor` là opaque và chỉ dùng lại với cùng bộ lọc status.
- Kết quả sắp xếp theo `createdAt DESC, id DESC`, loại bản ghi soft-delete và trả envelope `items`, `nextCursor`, `hasMore`.
- Thiếu identity trả `401`; identity không có `ROLE_ADMIN` trả `403 ACCESS_DENIED`.
- Khi tạo/cập nhật catalog ở các UC tiếp theo, field `code` chỉ nhận `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`.

Admin detail:

```http
GET /api/v1/admin/assessments/{assessmentId}
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_ADMIN
```

- Thành công trả `200 OK` với `id`, `code`, `assessmentVersion`, `status`, metadata, questions và ordered answer options của đúng version.
- Chấp nhận cả `DRAFT`, `PUBLISHED` và `ARCHIVED`; chỉ loại assessment/question/option đã soft-delete.
- ID không tồn tại/đã soft-delete trả `404 RESOURCE_NOT_FOUND`; thiếu quyền trả `401/403` theo security contract chung.

Create draft request:

```http
POST /api/v1/admin/assessments
Content-Type: application/json
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_ADMIN
```

```json
{
  "code": "PHQ-9",
  "title": "PHQ-9",
  "description": "Bài sàng lọc sức khỏe tinh thần.",
  "questions": [
    {
      "questionText": "Câu hỏi số 1",
      "orderIndex": 0
    }
  ]
}
```

- Thành công trả `201 Created`, `Location: /api/v1/admin/assessments/{id}` và toàn bộ draft đã persist.
- Version đầu tiên luôn là `1`, status luôn là `DRAFT`; client không được tự gửi version/status/ID.
- Admin không gửi `answerOptions`; nếu field này xuất hiện request trả `400 VALIDATION_ERROR`. Server tự sinh và persist response scale theo code, response vẫn trả options cùng ID để frontend/user sử dụng.
- `questions` có từ `1` đến số câu chuẩn của code; `orderIndex` phải liên tục `0..n-1`.
- Cấu trúc: PHQ-9 = 9 câu, 4 option `0..3`; GAD-7 = 7, 4 option `0..3`; DASS-21 = 21, 4 option `0..3`; PSS-10 = 10, 5 option `0..4`; WHO-5 = 5, 6 option `0..5`.
- Code đã tồn tại trả `409 ASSESSMENT_CODE_CONFLICT`; vượt số câu trả `400 ASSESSMENT_QUESTION_LIMIT_EXCEEDED`; thứ tự không liên tục trả `400 ASSESSMENT_QUESTION_ORDER_INVALID`; code ngoài catalog trả `400 MALFORMED_REQUEST`.

Update draft dùng cùng JSON schema với create:

```http
PUT /api/v1/admin/assessments/{assessmentId}
Content-Type: application/json
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_ADMIN
```

- Thành công trả `200 OK` và toàn bộ aggregate sau cập nhật.
- Đây là full replacement: questions/options active cũ được soft-delete, questions request được lưu và options mới được server sinh theo definition trong cùng transaction.
- `id`, `code`, `assessmentVersion` và `status` là bất biến; request vẫn phải gửi đúng `code` hiện tại.
- Chỉ status `DRAFT` được sửa. `PUBLISHED` hoặc `ARCHIVED` trả `409 INVALID_STATE_TRANSITION`; đổi code trả `409 ASSESSMENT_CODE_IMMUTABLE`; ID không tồn tại hoặc đã soft-delete trả `404 RESOURCE_NOT_FOUND`.
- Endpoint này không tự tạo version mới từ bản đã publish. Versioning là UC riêng.

Create next version:

```http
POST /api/v1/admin/assessments/{assessmentId}:create-next-version
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_ADMIN
```

- Request không có body. `{assessmentId}` phải là bản `PUBLISHED`; thành công trả `201 Created`, `Location: /api/v1/admin/assessments/{newDraftId}` và aggregate draft mới.
- Version mới bằng version lớn nhất hiện có của cùng code cộng một. Title, description và questions được clone; options được sinh lại theo definition hiện hành và persist thành snapshot mới.
- Source vẫn `PUBLISHED` và không bị sửa. Admin dùng `PUT /admin/assessments/{newDraftId}` để chỉnh draft trước khi publish.
- Nếu code đã có draft chờ trả `409 ASSESSMENT_DRAFT_VERSION_EXISTS`; source không published trả `409 INVALID_STATE_TRANSITION`; xung đột đồng thời trả `409 ASSESSMENT_VERSION_CONFLICT`; ID không tồn tại/đã soft-delete trả `404 RESOURCE_NOT_FOUND`.

Publish:

```http
POST /api/v1/admin/assessments/{assessmentId}:publish
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_ADMIN
```

- Request không có body. Thành công trả `200 OK` cùng aggregate có status `PUBLISHED`.
- Chỉ draft có đúng số câu chuẩn, thứ tự liên tục, mọi options khớp definition và code có scoring policy đã phê duyệt mới được publish.
- Thiếu câu trả `400 ASSESSMENT_INCOMPLETE`; sai scale trả `400 ASSESSMENT_SCALE_MISMATCH`; chưa có scoring policy trả `400 SCORING_POLICY_NOT_CONFIGURED`.
- Bản không ở `DRAFT` trả `409 INVALID_STATE_TRANSITION`; ID không tồn tại/đã soft-delete trả `404 RESOURCE_NOT_FOUND`.
- Trong cùng transaction, version published cũ của cùng code được archive trước để duy trì tối đa một version published.

Archive:

```http
POST /api/v1/admin/assessments/{assessmentId}:archive
X-User-Id: <verified-user-uuid>
X-User-Role: ROLE_ADMIN
```

- Request không có body. Thành công trả `200 OK` cùng aggregate có status `ARCHIVED`.
- Chỉ assessment `PUBLISHED` được archive. Draft hoặc bản đã archived trả `409 INVALID_STATE_TRANSITION`.
- Archive không xóa catalog hoặc result lịch sử; ID không tồn tại/đã soft-delete trả `404 RESOURCE_NOT_FOUND`.

## Risk và truy cập nội bộ

Health benchmark additions:

- `GET /api/v1/health-metrics/benchmark-evaluations`: đánh giá deterministic cho ngủ, bước chân, nhịp tim lúc nghỉ và SpO₂.
- `POST /api/v1/risk-alerts/analyze-health`: persist cảnh báo health benchmark mới theo cooldown và trả danh sách alert vừa tạo.
- `GET /api/v1/self-care-plan/templates` và `GET /api/v1/self-care-plan/recommendations`: catalog cố định và đề xuất plan.
- `POST /api/v1/self-care-plan/templates/{templateCode}:apply`: áp dụng một template nguyên vẹn.

| Method | Path | Caller | Mục đích |
|---|---|---|---|
| POST | `/internal/risk-analyses` | scheduler/service account | Kích hoạt phân tích idempotent khi cần |
| GET | `/risk-alerts` | USER | Xem cảnh báo của chính mình |
| POST | `/internal/users/{userId}/shared-summary:authorize-and-read` | Booking Service/Expert flow | Trả snapshot tối thiểu sau authorization |

Endpoint shared summary chỉ là placeholder contract. Phương án ưu tiên là Gateway/service-to-service identity cộng authorization proof ngắn hạn từ Booking Service; không triển khai trước khi chốt consent và threat model.

## Concurrency và idempotency

- Server lưu hash request theo idempotency key/scope. Cùng key + cùng payload trả kết quả cũ; cùng key + payload khác trả `409 IDEMPOTENCY_KEY_REUSED`.
- Journal không hỗ trợ PATCH. Aggregate khác nếu cho phép concurrent edit phải dùng optimistic version/ETag phù hợp.
- Assessment submit, lưu result và enqueue outbox event phải cùng transaction.

## OpenAPI checklist

- Mỗi operation có operation ID, role/scope, schema, example và error codes.
- Enum, min/max/length/pattern được mô tả ở schema và validate runtime.
- Không ghi schema nội bộ như `answers_detail` nguyên trạng vào public contract.
- Contract test bảo vệ compatibility và response không lộ field nhạy cảm.
