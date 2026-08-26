# Luật nghiệp vụ Emotion Service

## Nguyên tắc chung

- `userId` của thao tác self-service lấy từ security context, không lấy từ payload.
- UUID là định danh công khai. Tất cả entity có `created_at`, `updated_at`, `deleted_at`; bản ghi đã xóa mềm không xuất hiện trong truy vấn thông thường.
- Thời gian lưu UTC bằng PostgreSQL `TIMESTAMPTZ` và ánh xạ `OffsetDateTime`; `recordedAt` là thời điểm phép đo xảy ra, `createdAt` là thời điểm hệ thống nhận/lưu.
- Không sửa dữ liệu lịch sử theo cách làm mất khả năng truy vết. Correction phải có audit hoặc tạo phiên bản/bản ghi thay thế.

## Nhật ký cảm xúc

### Loại cảm xúc baseline

`VERY_HAPPY`, `HAPPY`, `NEUTRAL`, `SAD`, `STRESSED` theo tài liệu entity. Nếu product cần thang chi tiết hơn, phải version mapping trước khi phát hành vì trend phụ thuộc mapping này.

### Quy tắc

1. `emotionType` bắt buộc và thuộc catalog/enum đang hoạt động.
2. `content` tùy chọn, tối đa 5.000 Unicode code point, normalize xuống dòng/khoảng trắng và xử lý an toàn khi hiển thị.
3. Server gán `createdAt`; client có thể cung cấp `occurredAt` trong thiết kế tương lai để hỗ trợ offline, nhưng schema V1 chưa có trường này.
4. User chỉ xem/xóa nhật ký của mình. Journal không có use case sửa vì phản ánh trạng thái tại đúng thời điểm ghi nhận. Expert chỉ xem dữ liệu tổng hợp/chi tiết theo consent và quan hệ tư vấn hợp lệ.
5. User chỉ được soft delete journal từ thời điểm tạo đến hết đúng phút thứ 15 (`now <= createdAt + 15 phút`). Sau cửa sổ này trả `JOURNAL_DELETION_WINDOW_EXPIRED`; việc khôi phục, retention và xóa vật lý cần policy riêng.
6. Trong giai đoạn test chưa áp dụng giới hạn tần suất tạo journal. Rate limit chống spam là production prerequisite và phải được chốt riêng.
7. “Tiêu cực liên tục” không được suy ra chỉ bằng chuỗi string; phải dùng mapping điểm và rule version đã công bố.

## Tổng hợp xu hướng

1. Khoảng thời gian bắt buộc có `from <= to` và không vượt giới hạn truy vấn được cấu hình.
2. Bucket `DAY`/`WEEK`/`MONTH` được tính theo timezone IANA hợp lệ; tuần bắt đầu vào thứ Hai, tháng bắt đầu ngày 1 và dữ liệu lưu vẫn là UTC.
3. Mapping điểm cảm xúc phải cố định theo version. Không so sánh hai giai đoạn dùng mapping khác mà không normalize.
4. Bucket không có dữ liệu trả `count = 0` và `averageScore = null`, không giả định cảm xúc trung tính.
5. API phải trả cả `count` để UI không diễn giải một mẫu đơn lẻ như xu hướng mạnh.
6. `emotion-v1` ánh xạ `VERY_HAPPY = 2`, `HAPPY = 1`, `NEUTRAL = 0`, `SAD = -1`, `STRESSED = -2`; `averageScore = tổng điểm / count`, làm tròn 2 chữ số theo `HALF_UP`.
7. Response tạo chuỗi bucket liên tục trên khoảng `[from, to)`; bucket không có journal vẫn xuất hiện. Khoảng trend tối đa 730 ngày và mặc định loại journal đã soft-delete.

## Dữ liệu sức khỏe

### Loại và đơn vị canonical

| Metric | Đơn vị canonical | Ràng buộc baseline |
|---|---|---|
| `SLEEP_HOURS` | `h` | Không âm; giới hạn sinh lý/ingest cụ thể cần cấu hình |
| `HEART_RATE` | `bpm` | Số dương; phân biệt sample và aggregate trong thiết kế mở rộng |
| `STEP_COUNT` | `count` | Số nguyên không âm dù schema hiện dùng numeric |
| `SLEEP_SESSION` | duration suy ra theo giờ | Bắt buộc `startTime < endTime`, tối đa cửa sổ ingest |
| `EXERCISE_SESSION` | duration suy ra theo giờ | Bắt buộc `startTime < endTime`, giữ `exerciseType` trong details |

Nguồn baseline: `APPLE_HEALTH`, `HEALTH_CONNECT` (`GOOGLE_HEALTH` là alias tương thích), `MANUAL`.

### Quy tắc

1. Client chỉ gửi dữ liệu sau khi hệ điều hành/user cấp quyền; server không thể tự cấp quyền native.
2. Metric point phải có value/unit; session phải có `startTime`/`endTime`. `recordedAt` hợp lệ và không ở tương lai quá clock-skew.
3. Convert về đơn vị canonical trước khi phân tích; giữ metadata nguồn cần thiết để audit.
4. Batch phải có `Idempotency-Key`; dữ liệu thiết bị bắt buộc có external sample ID. Cùng user/source/external ID được upsert, không tạo trùng; `sourceLastModifiedAt` mới hơn được quyền cập nhật bản ghi.
5. Trend theo ngày tổng hợp riêng từng metric: heart rate dùng average; steps và duration session dùng sum. Không trộn metric hoặc đơn vị.
6. Batch tối đa 100 item và được validate atomically; item không hợp lệ làm cả batch thất bại với error rõ ràng.
7. Thu hồi consent dừng ingest tương lai; xử lý dữ liệu đã lưu theo policy retention/erasure được duyệt.

## Assessment

### Loại assessment được hỗ trợ

Catalog hiện chỉ nhận năm code canonical: `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`. Application biểu diễn bằng `AssessmentCode`; JSON và PostgreSQL vẫn dùng chuỗi có dấu gạch ngang. Việc có code trong enum không đồng nghĩa scoring policy đã được phê duyệt: hiện chỉ PHQ-9 và GAD-7 được phép publish.

### Vòng đời catalog

`DRAFT -> PUBLISHED -> ARCHIVED` là lifecycle mục tiêu. Chỉ `PUBLISHED` được user thực hiện. Sau publish, cấu trúc, điểm và threshold là immutable; thay đổi tạo version mới.

### User xem danh sách assessment

1. Chỉ principal có `ROLE_USER` được gọi API assessment dành cho người làm bài; role admin/expert không tự động kế thừa quyền user.
2. Danh sách chỉ gồm assessment `PUBLISHED` và `deleted_at IS NULL`; draft/archived không được lộ.
3. Mỗi code tối đa có một version published theo constraint PostgreSQL. Kết quả sắp xếp ổn định theo code tăng dần.
4. Response summary chỉ chứa `id`, code canonical, assessment version, title và description; không trả questions, options, score hoặc lifecycle status.
5. Không phân trang ở baseline vì catalog là tập đóng tối đa năm code. Khi không có assessment published, trả mảng rỗng `[]`.

### User xem nội dung assessment

1. Chỉ `ROLE_USER`; URL nhận code canonical `PHQ-9`, `GAD-7`, `DASS-21`, `PSS-10`, `WHO-5`, không nhận tên enum Java.
2. Chỉ version `PUBLISHED` active của code được trả. Code hợp lệ nhưng không có version published, hoặc version đã archive/soft-delete, trả `RESOURCE_NOT_FOUND`.
3. Questions và answer options active được sắp theo `orderIndex` tăng dần và thuộc đúng assessment version.
4. Response option chỉ gồm `id` và `optionText`; tuyệt đối không trả `scoreValue`. Response cũng không trả lifecycle status.
5. Code ngoài catalog trả `MALFORMED_REQUEST` trước khi gọi application service.

### Tạo draft catalog

1. Chỉ `ROLE_ADMIN` được tạo catalog; create lần đầu luôn tạo `assessmentVersion = 1`, trạng thái `DRAFT`.
2. Một code đã có bất kỳ version active nào không được tạo lại bằng create; version tiếp theo phải đi qua update/versioning use case.
3. Assessment, questions và answer options được lưu trong cùng transaction; lỗi ở một phần rollback toàn bộ aggregate.
4. Admin chỉ gửi nội dung và `orderIndex` câu hỏi, không gửi answer options. Server lấy response scale từ `AssessmentDefinitionRegistry`, sinh options giống nhau cho từng câu và persist chúng thành snapshot của assessment version.
5. Draft phải có ít nhất một câu và không được vượt số câu chuẩn của code. `orderIndex` phải liên tục từ `0` đến `n-1`; title/question text được trim và không được blank.
6. Cấu trúc chuẩn: PHQ-9 = 9 câu/4 lựa chọn `0..3`; GAD-7 = 7/4 `0..3`; DASS-21 = 21/4 `0..3`; PSS-10 = 10/5 `0..4`; WHO-5 = 5/6 `0..5`.
7. `scoreValue` được lưu như raw value của response scale. Phép đảo điểm PSS, subscale DASS hoặc chiều ý nghĩa WHO-5 thuộc scoring policy theo code/version, không thuộc answer option hoặc mapper.
8. Các nhãn tiếng Việt trong registry là nội dung cấu hình hiện tại và vẫn cần clinical/localization review trước production.

### Tra cứu catalog cho admin

1. Chỉ `ROLE_ADMIN` được xem chi tiết catalog quản trị.
2. Admin được xem assessment ở mọi lifecycle status `DRAFT`, `PUBLISHED`, `ARCHIVED`, cùng toàn bộ questions và ordered answer options active của đúng version.
3. Assessment hoặc child đã soft-delete không xuất hiện trong response. ID không tồn tại/đã soft-delete trả `RESOURCE_NOT_FOUND`.
4. Đây là read-only use case, không khóa pessimistic và không làm thay đổi audit timestamp hay lifecycle.

### Cập nhật draft catalog

1. Chỉ `ROLE_ADMIN` được cập nhật và chỉ assessment đang ở trạng thái `DRAFT` được chấp nhận.
2. `PUT` là phép thay thế đầy đủ aggregate: title/description được cập nhật; questions và answer options active cũ được soft-delete, questions mới được lưu và options mới được server sinh từ definition trong cùng transaction.
3. `id`, `code`, `assessmentVersion` và `status` không thay đổi. Đổi code trả `ASSESSMENT_CODE_IMMUTABLE`; cập nhật `PUBLISHED`/`ARCHIVED` trả `INVALID_STATE_TRANSITION`.
4. Request dùng cùng validation/invariant với create draft và không được chứa answer options. Lỗi ở bất kỳ bước nào rollback toàn bộ, không để catalog ở trạng thái cập nhật dở.
5. Version đã publish không được sửa bằng endpoint này. Tạo draft cho version tiếp theo là UC versioning riêng, không phải side effect ngầm của update draft.

### Tạo draft version tiếp theo

1. Chỉ `ROLE_ADMIN` và chỉ assessment đang `PUBLISHED` được dùng làm source.
2. Hệ thống lấy version lớn nhất đang tồn tại của cùng code và tạo version `max + 1` ở trạng thái `DRAFT`; ID mới được sinh ở application.
3. Title, description và toàn bộ question active được sao chép. Answer options không sao chép mù từ database cũ mà được sinh lại theo `AssessmentDefinitionRegistry` hiện hành, sau đó persist làm snapshot của version mới.
4. Source `PUBLISHED`, questions/options và result lịch sử không bị sửa. Admin tiếp tục dùng endpoint update draft để chỉnh nội dung version mới.
5. Mỗi assessment code chỉ có tối đa một draft đang chờ. Nếu đã có draft trả `ASSESSMENT_DRAFT_VERSION_EXISTS`.
6. Thao tác khóa pessimistic bản version mới nhất trong transaction; unique `(code, assessment_version)` vẫn là lớp bảo vệ cuối. Xung đột tạo đồng thời trả `ASSESSMENT_VERSION_CONFLICT`.
7. Source không ở `PUBLISHED` trả `INVALID_STATE_TRANSITION`; ID không tồn tại hoặc soft-delete trả `RESOURCE_NOT_FOUND`.

### Điều kiện publish

1. Chỉ assessment `DRAFT` được publish.
2. Số câu phải đúng definition: PHQ-9 = 9, GAD-7 = 7, DASS-21 = 21, PSS-10 = 10, WHO-5 = 5.
3. Thứ tự câu phải liên tục từ `0`; options active của từng câu phải khớp tuyệt đối số lượng, thứ tự, nhãn và raw value trong definition.
4. Code phải có scoring policy được phê duyệt. Definition cấu trúc không đồng nghĩa assessment đã đủ điều kiện chuyên môn để publish; hiện chỉ PHQ-9/GAD-7 có scoring policy.
5. Thiếu câu trả `ASSESSMENT_INCOMPLETE`; sai scale trả `ASSESSMENT_SCALE_MISMATCH`; chưa có scoring policy trả `SCORING_POLICY_NOT_CONFIGURED`.
6. Publish chuyển trạng thái trong một transaction và không sửa nội dung catalog. Nếu đã có version cùng code đang publish, version đó được archive trước khi version mới trở thành `PUBLISHED`, bảo toàn constraint chỉ có một version published cho mỗi code.

### Archive catalog

1. Chỉ `ROLE_ADMIN` và chỉ assessment đang `PUBLISHED` được archive.
2. `DRAFT -> ARCHIVED` không hợp lệ vì sẽ bỏ qua publish gate; archive lại bản `ARCHIVED` cũng bị từ chối. Cả hai trả `INVALID_STATE_TRANSITION`.
3. Archive chỉ chuyển lifecycle sang `ARCHIVED`, không soft-delete assessment/questions/options và không xóa assessment result cũ.
4. Sau archive, version không còn xuất hiện trong public catalog hoặc nhận submission mới; dữ liệu lịch sử vẫn giữ nguyên để truy vết.

### Nộp bài và chấm điểm

1. `assessmentCode` và version phải tồn tại/đang cho phép nộp.
2. Mỗi câu hỏi bắt buộc có đúng một lựa chọn, trừ khi định nghĩa bài test nói khác.
3. Question/option phải thuộc đúng assessment version; không nhận `scoreValue` từ client.
4. Không chấp nhận câu hỏi trùng, thiếu hoặc không thuộc bài.
5. Server lấy score từ catalog, tính tổng và phân loại bằng threshold của cùng version trong một transaction.
6. Kết quả lưu snapshot/version đủ để tái tạo phép tính về sau.
7. Retry với cùng idempotency key trả cùng kết quả, không tạo bản ghi kép.
8. Kết quả là sàng lọc, kèm lời giải thích/khuyến nghị đã được duyệt; không dùng từ ngữ chẩn đoán.
9. Identity lấy từ `SecurityContext`; submission chỉ dành cho `ROLE_USER` và request không được gửi `userId` hoặc `scoreValue`.
10. `Idempotency-Key` bắt buộc, khác rỗng và tối đa 255 ký tự. Cùng key nhưng payload khác trả `IDEMPOTENCY_CONFLICT`.
11. Chỉ version `PUBLISHED` hiện hành của code nhận submission. Version request cũ/mới khác version hiện hành trả `ASSESSMENT_VERSION_CONFLICT`; code chưa có published version trả `RESOURCE_NOT_FOUND`.

### Lịch sử và chi tiết kết quả assessment

1. Chỉ `ROLE_USER` được dùng result API; `userId` lấy từ SecurityContext và không nhận từ query/body.
2. Lịch sử chỉ trả result active thuộc user hiện tại, theo khoảng `[from, to)`, tối đa 365 ngày.
3. Lịch sử dùng keyset pagination theo `(created_at, id)` giảm dần, limit `1..100`; cursor bị ràng buộc với user và đúng khoảng thời gian truy vấn.
4. Chi tiết lọc đồng thời `id`, `user_id` và `deleted_at IS NULL`. Result không tồn tại, đã xóa hoặc thuộc user khác đều trả cùng `404 RESOURCE_NOT_FOUND`.
5. Response chỉ trả kết quả sàng lọc đã lưu gồm code/version, tổng điểm, risk level, screening notice, recommendations và thời điểm tạo; không trả answer snapshot hoặc scoring internals.

### Mức nguy cơ baseline

`NORMAL`, `MILD`, `MODERATE`, `SEVERE`, `EXTREME`. Khoảng điểm cụ thể khác nhau theo bài test và không được hard-code chung cho DASS-21, PHQ-9, GAD-7.

## Phân tích rủi ro tổng hợp

1. Input mục tiêu: cảm xúc 7 ngày, health metrics 3-7 ngày và assessment gần nhất.
2. Ví dụ trong yêu cầu (ngủ dưới 4 giờ trong 3 ngày + cảm xúc tiêu cực + resting heart rate cao) chỉ là ví dụ, không phải rule production đã được phê duyệt.
3. Mỗi lần phân tích phải ghi `ruleVersion`, cửa sổ dữ liệu, nhóm tín hiệu đã dùng, mức cảnh báo và reason code có thể giải thích.
4. Thiếu dữ liệu phải làm giảm confidence hoặc trả `INSUFFICIENT_DATA`; không tự điền giá trị “bình thường”.
5. Một cửa sổ/rule không tạo lặp cảnh báo vô hạn. Dùng deduplication key và cooldown được cấu hình.
6. Cảnh báo cao phải đưa ra hành động an toàn và kênh hỗ trợ con người; quy trình tự hại/khẩn cấp cần chuyên gia pháp lý/lâm sàng phê duyệt.
7. `is_notified` chỉ chuyển thành true sau xác nhận từ notification workflow; không đồng nhất “đã phát event” với “thiết bị đã nhận”.

## Quyền truy cập

| Tác nhân | Quyền mặc định |
|---|---|
| User | CRUD/xem dữ liệu của chính mình theo policy; làm và xem assessment của mình |
| Expert | Chỉ đọc phạm vi dữ liệu của user có booking/consent hợp lệ; không sửa dữ liệu gốc |
| Admin | Quản lý catalog assessment; không mặc nhiên được đọc nội dung tâm lý cá nhân |
| Service account | Chỉ scope tối thiểu cho contract nội bộ, có audit |

## Quy tắc còn mở

- DASS-21 chỉ được publish sau khi threshold/nội dung được phê duyệt từ manual và chuyên gia; baseline chạy hiện chỉ cấu hình PHQ-9 và GAD-7.
- Composite risk từ journal/health chưa được kích hoạt; `risk-v1` chỉ dùng kết quả assessment gần nhất trong 30 ngày và không tạo kết luận chẩn đoán.
- Quy trình `CRITICAL`, tự hại/khẩn cấp và nội dung liên hệ hỗ trợ cần clinical/legal approval trước production.
- Consent model, retention, export, hard delete và data residency.
- Ai sở hữu notification record và cơ chế xác nhận delivery.

## Baseline triển khai được duyệt ngày 2026-07-22

- Journal content tối đa 5.000 Unicode code point; chuẩn hóa newline, trim và chuyển blank thành `null`. Journal không có update use case; mapping `emotion-v1` dùng điểm `2, 1, 0, -1, -2` từ `VERY_HAPPY` đến `STRESSED`.
- Query dùng khoảng `[from, to)`, giới hạn trang 1-100, keyset cursor theo `(timestamp, id)`. Lịch sử tối đa 365 ngày; trend tối đa 730 ngày; bucket `DAY`, `WEEK`, `MONTH`, tuần bắt đầu thứ Hai.
- Health batch atomic, tối đa 100 item, clock skew tương lai 5 phút và backfill tối đa 30 ngày. External sample ID bắt buộc với Apple/Google; đơn vị canonical là `h`, `bpm`, `count`.
- Assessment lifecycle `DRAFT -> PUBLISHED -> ARCHIVED`; published version immutable. PHQ-9 dùng biên 5/10/15/20; GAD-7 dùng biên 5/10/15; scoring rule có version và result lưu snapshot.
- `risk-v1`: `SEVERE -> ELEVATED` với cooldown 72 giờ; `EXTREME -> HIGH` với cooldown 24 giờ. Mức thấp hơn hoặc thiếu assessment không tạo alert.
