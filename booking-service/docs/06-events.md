# Event Contract Booking Service

## Nguyên tắc

- REST cho thao tác cần phản hồi ngay; RabbitMQ/outbox cho notification và integration lỏng.
- Delivery giả định at-least-once; consumer phải idempotent.
- Event có `eventId`, `eventType`, `eventVersion`, `occurredAt`, `correlationId`, aggregate ID.
- Không chứa chat content, consultation note, JWT, payment signature/raw callback hoặc dữ liệu Emotion.
- Publish từ `outbox_events` chỉ sau khi transaction nghiệp vụ commit.

## Envelope

```json
{
  "eventId": "uuid",
  "eventType": "booking.confirmed",
  "eventVersion": 1,
  "occurredAt": "2026-07-25T08:00:00Z",
  "correlationId": "opaque-id",
  "aggregateType": "BOOKING",
  "aggregateId": "uuid",
  "payload": {}
}
```

## Event phát ra

| Event | Khi phát | Payload tối thiểu |
|---|---|---|
| `booking.payment-pending` | Slot được hold | bookingId, userId, expertUserId, scheduleId, startAt |
| `booking.confirmed` | Payment thành công trong hold | bookingId, userId, expertUserId, scheduleId |
| `booking.expired` | Hold hết hạn | bookingId, scheduleId |
| `booking.cancellation-requested` | User yêu cầu hủy trong `[2h,24h)` | bookingId, expertUserId, decisionDeadline |
| `booking.cancellation-rejected` | Expert từ chối hoặc timeout | bookingId, userId, reasonCode nếu có |
| `booking.canceled` | Actor/system hủy | bookingId, userId, expertUserId, actor, reasonCode |
| `booking.completed` | Tư vấn hoàn tất | bookingId, userId, expertUserId, completedAt |
| `payment.succeeded` | Callback hợp lệ | paymentId, bookingId, amount, currency, provider |
| `payment.failed` | Failed/expired | paymentId, bookingId, provider, reasonCode |
| `payment.refunded` | Refund provider thành công | paymentId, bookingId, refundId |
| `review.created` | Review hợp lệ | reviewId, bookingId, expertUserId, rating |
| `expert.rating-projection-updated` | Rating aggregate thay đổi | expertUserId, ratingAverage, reviewCount |
| `expert.message.created` | Message persist | messageId, conversationId, senderId, recipientId |

`expert.message.created` không chứa content; Notification capability tự tạo nội dung generic như “Bạn có tin nhắn mới”.

## Event nhận vào dự kiến

| Event | Nguồn | Mục đích |
|---|---|---|
| `auth.expert.approved` | Auth | Cho phép tạo schedule/booking mới theo expert ID |
| `auth.expert.profile-updated` | Auth | Làm mới cache profile/fee nếu có |
| `auth.expert.blocked` | Auth | Ngăn schedule/booking mới, xử lý lịch tương lai theo policy |
| `emotion.expert-recommendation-requested` | Emotion/AI | Hỗ trợ filter catalog, không tự tạo booking |

Consumer/inbox chưa có schema riêng trong V1 ngoài payment webhook receipt. Khi triển khai RabbitMQ consumer nghiệp vụ, bổ sung `inbox_events` bằng migration mới hoặc một cơ chế dedup tương đương.

## Retry và DLQ

- Exponential backoff có giới hạn.
- Sau số lần retry cấu hình, chuyển DLQ và phát metric/alert.
- Chỉ đánh dấu outbox `PUBLISHED` sau broker confirm.
- Không xóa outbox ngay; retention/partitioning cần quyết định vận hành.
