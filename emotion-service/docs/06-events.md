# Event Contract mục tiêu

> Trạng thái: thiết kế baseline, **RabbitMQ chưa được cấu hình/triển khai trong repository**.

## Khi nào dùng event

Dùng REST cho truy vấn hoặc thao tác cần kết quả ngay. Dùng event cho notification, phân tích nền, cache invalidation và tích hợp lỏng. Event là fact đã xảy ra; command/request bất đồng bộ phải được đặt tên rõ, không giả làm fact.

Delivery model giả định `at-least-once`; không dựa vào exactly-once.

## Envelope chuẩn

```json
{
  "eventId": "6603475a-7526-48b3-909d-e73b15475682",
  "eventType": "assessment.completed.v1",
  "occurredAt": "2026-07-22T08:10:00Z",
  "producer": "emotion-service",
  "correlationId": "01J...",
  "causationId": "01J...",
  "subject": "assessment-result/5a36723f-b163-4f88-946e-7937374cfb0c",
  "schemaVersion": 1,
  "data": {}
}
```

- `eventId` duy nhất toàn cục.
- `correlationId` nối request/flow; `causationId` trỏ command/event gây ra.
- `subject` dùng opaque resource ID, không chứa email/tên.
- Header có content type, trace context và retry count theo platform convention.

## Event do Emotion Service phát

| Routing key / type | Khi phát | Payload tối thiểu | Consumer dự kiến |
|---|---|---|---|
| `emotion.journal.recorded.v1` | Journal đã commit | `journalId`, opaque `userId`, `emotionType`, `recordedAt` | risk analyzer nội bộ |
| `health.metrics.ingested.v1` | Batch đã commit | `batchId`, opaque `userId`, danh sách type, time range, count | risk analyzer |
| `assessment.completed.v1` | Result đã commit | `resultId`, opaque `userId`, `assessmentCode`, version, `riskLevel`, `completedAt` | risk/analytics |
| `risk.alert.created.v1` | Alert mới, không bị dedup | `alertId`, opaque `userId`, `level`, reason codes, rule version, suggested action codes | notification, safety workflow |
| `assessment.catalog.published.v1` | Version mới publish | code, version, publishedAt | cache/admin analytics |

Không đưa `content` journal, raw metric values, câu trả lời chi tiết hoặc trigger narrative tự do vào event mặc định.

## Command/event Emotion Service gửi cho notification

Nếu platform dùng queue command, tên đề xuất `notification.requested.v1`:

```json
{
  "notificationId": "1b62f569-a78f-4792-a908-3cdca9114af3",
  "recipientUserId": "8e864794-dfa0-4388-85fc-82ab534b6927",
  "category": "PSYCHOLOGICAL_RISK_ALERT",
  "templateKey": "risk-support-v1",
  "templateParameters": {"level": "HIGH"},
  "actionCodes": ["BREATHING_EXERCISE", "CHAT_AI", "BOOK_EXPERT"],
  "deduplicationKey": "risk-alert:..."
}
```

Nội dung hiển thị nên được quản lý bằng template đã duyệt ở notification capability; không phát nội dung tâm lý tự do qua broker nếu có thể tránh.

## Event Emotion Service có thể consume

| Type | Producer | Hành vi |
|---|---|---|
| `user.erasure.requested.v1` | Auth/Privacy workflow | Xác minh scope, xóa mềm/anonymize theo policy, phát completion/failure |
| `consultation.access.granted.v1` | Booking Service | Tạo/cập nhật authorization projection có expiry nếu kiến trúc chọn event-driven consent |
| `consultation.access.revoked.v1` | Booking Service | Thu hồi projection ngay, idempotent |
| `notification.delivery.updated.v1` | Notification capability | Liên kết delivery status với alert; không giả định delivered chỉ vì published |

Danh sách này chưa phải cam kết tích hợp; ownership và schema cần được các service cùng duyệt.

## Tin cậy

### Producer

- Dùng transactional outbox để DB state và event không lệch nhau.
- Publisher retry có backoff; record chỉ đánh dấu published sau broker confirm.
- Không publish trực tiếp trước transaction commit.

### Consumer

- Inbox/dedup theo `eventId` hoặc business dedup key.
- Xử lý idempotent trong transaction; acknowledge sau commit.
- Retry lỗi tạm thời; lỗi schema/business không thể phục hồi chuyển DLQ kèm metadata an toàn.
- Có giới hạn retry, alert vận hành và quy trình replay DLQ.

## Versioning

- Tên type có hậu tố `.v1`; thay đổi additive giữ version nếu consumer cũ bỏ qua field lạ.
- Xóa/đổi nghĩa/đổi type field là breaking và tạo `.v2`.
- Producer hỗ trợ overlap khi migration; consumer contract test với schema registry hoặc JSON Schema được version hóa.
- Không dùng Java class serialization làm wire format.

## Quan sát và bảo mật

- Metric: publish latency, consumer lag, retry, DLQ count, handler duration, dedup count.
- Trace propagation qua message headers.
- Broker permission theo least privilege cho exchange/queue.
- Mã hóa in transit/at rest theo môi trường; không log payload nhạy cảm.
- Retention queue/DLQ phải phù hợp data retention policy.
