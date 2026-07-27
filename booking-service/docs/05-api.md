# API Booking Service

## Trạng thái

Các endpoint không liên quan payment trong tài liệu này đã có Controller và MockMvc integration test. Payment API/provider callback và WebSocket realtime chưa được triển khai. REST chat hiện là transport dùng được cho frontend/test.

Base path: `/api/v1`.

## Identity và header

Gateway xác thực JWT và chuyển verified identity:

```http
X-User-Id: <uuid>
X-User-Role: ROLE_USER | ROLE_EXPERT | ROLE_ADMIN
X-Correlation-Id: <opaque-id>
```

Gateway phải loại header cùng tên do client bên ngoài tự gửi. Controller dùng SecurityContext, không nhận `userId` cho thao tác của chính actor.

Các write operation có nguy cơ retry bắt buộc:

```http
Idempotency-Key: <unique-key-up-to-255-characters>
```

## Error envelope

```json
{
  "type": "https://api.mindcare.vn/problems/resource-conflict",
  "title": "Resource conflict",
  "status": 409,
  "code": "SCHEDULE_NOT_AVAILABLE",
  "detail": "The selected schedule is no longer available",
  "traceId": "opaque-correlation-id",
  "fieldErrors": []
}
```

## Expert catalog thuộc Auth Service

Auth Service vẫn sở hữu dữ liệu expert. Booking Service cung cấp facade đọc phân trang tại `GET /experts`, lấy dữ liệu qua `ExpertDirectoryGateway`; không persist hoặc CRUD expert profile. Adapter `local` chỉ dùng dữ liệu giả định danh cho local/Postman.

| Method | Path | Role | Mục đích |
|---|---|---|---|
| GET | `/experts` | USER | Danh sách expert bookable theo keyword/specialty, keyset cursor |
| GET | `/experts/{expertUserId}/schedules` | Public/User | Slot available trong khoảng thời gian |

## Schedule

| Method | Path | Role | Mục đích |
|---|---|---|---|
| POST | `/expert/schedules` | EXPERT | Tạo slot |
| GET | `/expert/schedules` | EXPERT | Lịch của expert |
| PUT | `/expert/schedules/{scheduleId}` | EXPERT | Sửa slot available |
| DELETE | `/expert/schedules/{scheduleId}` | EXPERT | Cancel slot available |

Request thời gian ISO-8601 có offset. Server chuẩn hóa UTC và từ chối overlap.

## Booking phía User

| Method | Path | Role | Mục đích |
|---|---|---|---|
| POST | `/bookings` | USER | Giữ slot; tạo payment nếu payment được bật, hoặc confirm ngay trong local test |
| GET | `/bookings` | USER | History của user, status/time filter, keyset |
| GET | `/bookings/{bookingId}` | USER | Chi tiết booking thuộc user |
| POST | `/bookings/{bookingId}:cancel` | USER | Hủy theo cancellation policy |

Create request chỉ chứa:

```json
{
  "scheduleId": "9c47f6d5-20aa-4fee-a79a-3f283ed849e5",
  "note": "Tôi muốn trao đổi về căng thẳng học tập.",
  "paymentMethod": null
}
```

Request bắt buộc `Idempotency-Key`. Server lấy user từ SecurityContext, xác minh expert và lấy price/currency qua Auth. Khi `BOOKING_PAYMENT_REQUIRED=false`, response có booking `CONFIRMED`, `payment=null`, `paymentRequired=false`; `paymentMethod` không bắt buộc. Cấu hình mặc định vẫn là `true` để fail-safe.

Cancel:

- `>=24h`: trả booking `CANCELED` và refund `PENDING`;
- `[2h,24h)`: trả booking `CANCELLATION_PENDING`;
- `<2h`: `409 CANCELLATION_WINDOW_CLOSED`.

## Booking phía Expert

| Method | Path | Role | Mục đích |
|---|---|---|---|
| GET | `/expert/bookings` | EXPERT | Booking history/inbox của expert |
| GET | `/expert/bookings/{bookingId}` | EXPERT | Chi tiết booking thuộc expert |
| POST | `/expert/bookings/{bookingId}/cancellation:decide` | EXPERT | Duyệt/từ chối yêu cầu hủy |
| POST | `/expert/bookings/{bookingId}:cancel` | EXPERT | Expert hủy và tạo full refund |
| POST | `/expert/bookings/{bookingId}:complete` | EXPERT | Hoàn tất sau `endAt` |
| POST | `/expert/bookings/{bookingId}:user-no-show` | EXPERT | Ghi nhận sau grace 15 phút |

## Payment

| Method | Path | Role | Mục đích |
|---|---|---|---|
| GET | `/bookings/{bookingId}/payments` | USER | Lịch sử payment của booking thuộc user |
| GET | `/payments/{paymentId}` | USER | Chi tiết payment thuộc user |
| POST | `/payment-webhooks/{provider}` | Provider | Callback/IPN ký số |

Payment attempt được tạo cùng checkout booking và không nhận amount/currency từ client. Webhook không dùng JWT user; adapter provider phải xác minh signature trên raw body rồi mới tạo `VerifiedPaymentCallback`. Late success tạo refund 100%, không xác nhận lại booking.

## Consultation note và review

| Method | Path | Role | Mục đích |
|---|---|---|---|
| PUT | `/expert/bookings/{bookingId}/consultation-note` | EXPERT | Tạo/cập nhật note khi được phép |
| GET | `/bookings/{bookingId}/consultation-note` | USER | Xem phần note visible |
| POST | `/bookings/{bookingId}/review` | USER | Review booking completed |
| GET | `/experts/{expertUserId}/reviews` | Public/User | Review public đã moderation |

## Expert chat

REST phục vụ bootstrap/history:

| Method | Path | Role |
|---|---|---|
| POST | `/bookings/{bookingId}/conversation` | USER/EXPERT participant |
| GET | `/conversations/{conversationId}/messages` | Participant |
| POST | `/conversations/{conversationId}/messages` | Participant; fallback/non-realtime |
| POST | `/conversations/{conversationId}:read` | Participant |

WebSocket mục tiêu:

```text
/ws/expert
/app/conversations/{conversationId}/messages
/topic/conversations/{conversationId}
```

Authentication phải gắn principal từ JWT/verified handshake; không authorize chỉ bằng `conversationId`.

## Internal authorization

Booking Service cần contract nội bộ để xác nhận một expert có quan hệ tư vấn với user trước khi Emotion Service chia sẻ dữ liệu. Response tối thiểu:

```json
{
  "authorized": true,
  "bookingId": "uuid",
  "scope": ["EMOTION_TREND", "ASSESSMENT_RESULT"],
  "validUntil": "2026-08-01T10:00:00Z"
}
```

Endpoint/service identity và consent source chưa chốt; không expose endpoint này cho client thông thường.
