# Postman test cases cho Booking Service

## 1. Chạy service ở chế độ local

```powershell
$env:DB_PASSWORD="<local-postgres-password>"
$env:BOOKING_AUTH_ADAPTER_MODE="local"
$env:BOOKING_PAYMENT_REQUIRED="false"
mvn.cmd spring-boot:run
```

Payment mặc định vẫn bật. Chỉ đặt `BOOKING_PAYMENT_REQUIRED=false` cho local/test.

Để test nhanh các UC phụ thuộc thời gian, có thể rút ngắn policy trong đúng phiên local:

```powershell
$env:BOOKING_CANCELLATION_CLOSED_CUTOFF="10s"
$env:BOOKING_AUTOMATIC_REFUND_CUTOFF="5m"
$env:BOOKING_CANCELLATION_DECISION_TIMEOUT="30s"
$env:BOOKING_NO_SHOW_GRACE_PERIOD="10s"
$env:BOOKING_COMPLETION_REVIEW_TIMEOUT="10m"
```

## 2. Postman variables

| Variable | Giá trị gợi ý |
|---|---|
| `baseUrl` | `http://localhost:8083/api/v1` |
| `userId` | `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa` |
| `expertId` | `11111111-1111-4111-8111-111111111111` |
| `otherUserId` | `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb` |
| `scheduleId` | Lấy từ API tạo slot |
| `bookingId` | Lấy từ `booking.id` của API tạo booking |
| `conversationId` | Lấy từ API tạo/lấy conversation |
| `from` | ISO-8601 trước thời gian slot, ví dụ `2026-07-26T00:00:00+07:00` |
| `to` | ISO-8601 sau thời gian slot |

Header cho User:

```http
X-User-Id: {{userId}}
X-User-Role: ROLE_USER
X-Correlation-Id: postman-{{$guid}}
```

Header cho Expert:

```http
X-User-Id: {{expertId}}
X-User-Role: ROLE_EXPERT
X-Correlation-Id: postman-{{$guid}}
```

## 3. Thứ tự test và expected result

### A. Expert directory và schedule

1. `GET {{baseUrl}}/experts?limit=2`
   - Header User.
   - Mong đợi `200`, `items` tối đa 2, có `nextCursor` khi còn dữ liệu.
   - Gửi tiếp `cursor={{nextCursor}}`; không được trùng item trang trước.
   - Negative: role Expert nhận `403 ACCESS_DENIED`.

2. `POST {{baseUrl}}/expert/schedules`
   - Header Expert.
   - Body:

```json
{
  "startAt": "2026-07-26T09:00:00+07:00",
  "endAt": "2026-07-26T10:00:00+07:00"
}
```

   - Dùng thời gian tương lai thực tế khi test.
   - Mong đợi `201`, `Location`, status `AVAILABLE`; lưu `id` vào `scheduleId`.
   - Negative: tạo slot overlap nhận `409 SCHEDULE_OVERLAP`; thời gian quá khứ nhận `400`.

3. `GET {{baseUrl}}/expert/schedules?from={{from}}&to={{to}}&limit=50`
   - Header Expert.
   - Mong đợi `200`, chỉ dữ liệu thuộc `expertId`.

4. `GET {{baseUrl}}/experts/{{expertId}}/schedules?from={{from}}&to={{to}}&limit=50`
   - Không cần identity header.
   - Mong đợi `200`, chỉ slot `AVAILABLE`.

5. `PUT {{baseUrl}}/expert/schedules/{{scheduleId}}`
   - Header Expert; body cùng dạng create với khoảng tương lai mới.
   - Mong đợi `200` và thời gian mới.
   - Negative: actor Expert khác nhận `404`; slot đã booked nhận conflict.

6. `DELETE {{baseUrl}}/expert/schedules/{{scheduleId}}`
   - Header Expert.
   - Mong đợi `204`.
   - Negative: slot đã held/booked không được cancel.

Không dùng slot đã xóa ở bước 6 để tạo booking; hãy tạo một slot mới và lưu ID mới.

### B. Booking phía User

7. `POST {{baseUrl}}/bookings`
   - Header User và `Idempotency-Key: postman-booking-{{$guid}}`.
   - Body khi payment local bị tắt:

```json
{
  "scheduleId": "{{scheduleId}}",
  "note": "Trao đổi về căng thẳng công việc",
  "paymentMethod": null
}
```

   - Mong đợi `201`, `booking.status=CONFIRMED`, `booking.paymentStatus=UNPAID`, `payment=null`, `paymentRequired=false`; lưu `booking.id`.
   - Negative: thiếu `Idempotency-Key` nhận `400`; chọn lại slot đã booked nhận `409 SCHEDULE_NOT_AVAILABLE`; User trùng `expertId` nhận `409 SELF_BOOKING_NOT_ALLOWED`.

8. `GET {{baseUrl}}/bookings?limit=20`
   - Header User.
   - Mong đợi `200`, chỉ booking của User; dùng `nextCursor` để lấy trang kế.
   - Có thể lọc `status=CONFIRMED`.

9. `GET {{baseUrl}}/bookings/{{bookingId}}`
   - Header User.
   - Mong đợi `200`.
   - Negative: `otherUserId` nhận `404`, không tiết lộ ownership.

10. `POST {{baseUrl}}/bookings/{{bookingId}}:cancel`
    - Header User.
    - Body: `{"reason":"Không thể tham dự"}`.
    - Policy mặc định:
      - trước ít nhất 24h: `CANCELED`;
      - từ 2h đến dưới 24h: `CANCELLATION_PENDING`;
      - dưới 2h: `409 CANCELLATION_WINDOW_CLOSED`.
    - Khi payment local tắt, không tạo refund.

### C. Booking phía Expert

11. `GET {{baseUrl}}/expert/bookings?limit=20`
    - Header Expert.
    - Mong đợi `200`, chỉ booking thuộc Expert; hỗ trợ `status` và `cursor`.

12. `GET {{baseUrl}}/expert/bookings/{{bookingId}}`
    - Header Expert.
    - Mong đợi `200`; Expert khác nhận `404`.

13. `POST {{baseUrl}}/expert/bookings/{{bookingId}}/cancellation:decide`
    - Prerequisite: booking đang `CANCELLATION_PENDING`.
    - Body duyệt: `{"approved":true,"reason":"Đồng ý hỗ trợ hủy"}`.
    - Body từ chối: `{"approved":false,"reason":"Không đủ điều kiện"}`.
    - Mong đợi `CANCELED` nếu duyệt hoặc trở lại `CONFIRMED` nếu từ chối.

14. `POST {{baseUrl}}/expert/bookings/{{bookingId}}:cancel`
    - Header Expert; body `{"reason":"Chuyên gia có việc đột xuất"}`.
    - Mong đợi `200`, status `CANCELED`; local unpaid không tạo refund.

15. `POST {{baseUrl}}/expert/bookings/{{bookingId}}:complete`
    - Prerequisite: booking `CONFIRMED` và hiện tại đã qua `endAt`.
    - Mong đợi `200`, status `COMPLETED`.
    - Gọi sớm nhận `409 BOOKING_NOT_COMPLETABLE`.

16. `POST {{baseUrl}}/expert/bookings/{{bookingId}}:user-no-show`
    - Prerequisite: booking `CONFIRMED`, đã qua `startAt + no-show-grace`.
    - Mong đợi `200`, status `USER_NO_SHOW`.
    - Gọi sớm nhận `409 NO_SHOW_GRACE_PERIOD_ACTIVE`.

Các UC 13-16 làm thay đổi lifecycle và loại trừ lẫn nhau. Tạo booking riêng cho từng nhánh.

### D. REST chat

17. `POST {{baseUrl}}/bookings/{{bookingId}}/conversation`
    - Header User hoặc Expert participant.
    - Prerequisite: booking trong cửa sổ từ 24h trước start đến 24h sau end.
    - Mong đợi `200`; gọi lại trả cùng conversation; lưu `id`.

18. `POST {{baseUrl}}/conversations/{{conversationId}}/messages`
    - Header participant.
    - Body `{"content":"Xin chào, tôi muốn xác nhận lịch tư vấn."}`.
    - Mong đợi `200`.
    - Negative: nội dung trống nhận `400`; actor ngoài booking nhận `404`; ngoài cửa sổ chat nhận `409 CHAT_WINDOW_CLOSED`.

19. `GET {{baseUrl}}/conversations/{{conversationId}}/messages?limit=50`
    - Header participant.
    - Mong đợi `200`, lịch sử phân trang keyset.

20. `POST {{baseUrl}}/conversations/{{conversationId}}:read`
    - Header participant nhận tin.
    - Mong đợi `200`, body có `updatedMessages`.

### E. Consultation note

21. `PUT {{baseUrl}}/expert/bookings/{{bookingId}}/consultation-note`
    - Header Expert; booking `CONFIRMED` hoặc `COMPLETED` chưa quá thời hạn khóa note.
    - Body:

```json
{
  "observation": "Người dùng chia sẻ tình trạng căng thẳng.",
  "recommendation": "Duy trì lịch ngủ và bài tập thở.",
  "recoveryPlan": "Theo dõi trong 7 ngày.",
  "visibleToUser": true
}
```

   - Mong đợi `200`; PUT lại cập nhật cùng note.

22. `GET {{baseUrl}}/expert/bookings/{{bookingId}}/consultation-note`
    - Header Expert đúng booking.
    - Mong đợi `200`, thấy đầy đủ trường.

23. `GET {{baseUrl}}/bookings/{{bookingId}}/consultation-note`
    - Header User đúng booking.
    - Mong đợi `200`, chỉ phần cho User.
    - Nếu `visibleToUser=false`, User nhận `404`.

### F. Review

24. `POST {{baseUrl}}/bookings/{{bookingId}}/review`
    - Header User; booking phải `COMPLETED`.
    - Body `{"rating":5,"comment":"Buổi tư vấn hữu ích."}`.
    - Mong đợi `201` và `Location`.
    - Negative: gửi lần hai nhận `409 REVIEW_ALREADY_EXISTS`; booking chưa completed nhận `409 BOOKING_NOT_REVIEWABLE`; rating ngoài 1-5 nhận `400`.

25. `GET {{baseUrl}}/experts/{{expertId}}/reviews?limit=20`
    - Public, không cần identity header.
    - Mong đợi `200`, chỉ review active/public; dùng `nextCursor` cho trang kế.

## 4. API cố ý chưa test trong giai đoạn này

- Payment history/detail, checkout provider và webhook callback.
- WebSocket realtime.
- Internal consent authorization và Admin analytics.

Các phần này cần contract/provider hoặc quyết định còn mở; REST chat và payment-bypass hiện đủ để frontend phát triển/test các luồng không thanh toán.
