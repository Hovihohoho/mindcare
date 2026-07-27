# Quy tắc nghiệp vụ Booking Service

## Expert identity và profile

1. Auth Service sở hữu account, role và toàn bộ expert profile/catalog; Booking Service không tạo, cập nhật hoặc duyệt profile.
2. Booking chỉ lưu external `expertUserId`; không tạo FK xuyên service/schema.
3. Trước thao tác tạo slot hoặc booking mới, expert phải có role/trạng thái đủ điều kiện theo contract Auth.
4. Giá/currency dùng để tạo booking được lấy từ contract Auth phía server và snapshot vào booking; không nhận từ client.
5. Mất quyền expert hoặc profile bị khóa ngăn slot/booking mới. Cách xử lý lịch tương lai đã tồn tại cần contract event và policy riêng.
6. Rating/review do Booking sở hữu; Auth có thể nhận rating projection qua event, nhưng review gốc vẫn ở Booking.

## Schedule

1. `startAt < endAt`, dùng UTC và chỉ tạo slot tương lai.
2. Expert chỉ quản lý schedule có `expertUserId` trùng identity trong SecurityContext.
3. Slot active của cùng expert không được overlap theo khoảng `[startAt, endAt)`.
4. Service phải lấy PostgreSQL transaction-level advisory lock ổn định theo `expertUserId` trước khi kiểm tra/tạo slot để serialize concurrent inserts; exact duplicate còn được DB index bảo vệ.
5. Lifecycle:

```text
AVAILABLE -> HELD -> BOOKED
AVAILABLE -> CANCELLED
HELD -> AVAILABLE
BOOKED -> AVAILABLE (booking bị hủy/thanh toán thất bại theo rule)
```

Không sửa thời gian của slot đã `HELD` hoặc `BOOKED`.

## Booking

1. User không gửi owner, status, price hoặc currency.
2. Tạo booking chỉ nhận slot `AVAILABLE`; service xác minh expert vẫn đủ điều kiện và lấy fee/currency qua contract Auth.
3. Trong transaction: khóa slot, tạo booking `PAYMENT_PENDING`, snapshot fee/currency, chuyển slot `HELD` trong 15 phút và ghi outbox.
4. Partial unique index đảm bảo một booking `PAYMENT_PENDING/CONFIRMED/CANCELLATION_PENDING` active trên một slot.
5. Expert mở slot đồng nghĩa đồng ý nhận lịch; không có accept/reject booking:

```text
PAYMENT_PENDING -> CONFIRMED
PAYMENT_PENDING -> PAYMENT_FAILED/EXPIRED
CONFIRMED -> CANCELLATION_PENDING -> CONFIRMED/CANCELED
CONFIRMED -> CANCELED/COMPLETED/USER_NO_SHOW
```

6. Payment thành công trong hold chuyển slot `BOOKED`; payment fail/expire giải phóng slot. Complete không xóa lịch sử.
7. User/expert history mặc định loại `deleted_at`, dùng keyset `(createdAt,id)` và filter status tùy chọn.
8. User khác, expert khác hoặc resource soft-delete đều trả non-disclosing 404.
9. User hủy:
   - còn `>=24h`: hủy ngay và refund 100%;
   - còn `[2h,24h)`: `CANCELLATION_PENDING`, expert phản hồi trong 60 phút và trước mốc 2 giờ; timeout tự từ chối;
   - còn `<2h`: không cho hủy.
10. Khi chờ duyệt, slot vẫn `BOOKED`. Expert hủy ở mọi thời điểm trước buổi hẹn thì refund 100%.
11. Chỉ expert đúng booking được complete sau `endAt`, khi payment `PAID`. User no-show chỉ được ghi sau `startAt + 15 phút`.

## Payment

1. Chỉ booking thuộc user, có trạng thái cho phép và chưa `PAID` mới khởi tạo payment.
2. Amount/currency lấy từ booking snapshot; không tin giá từ client.
3. `Idempotency-Key` bắt buộc. Cùng key/cùng request trả attempt cũ; cùng key/payload khác trả conflict.
4. Mỗi booking tối đa một payment `PENDING`; có thể có nhiều attempt lịch sử.
5. Callback:
   - xác minh chữ ký trước mutation;
   - deduplicate `(provider, providerEventId)`;
   - so khớp order, amount, currency và trạng thái;
   - cập nhật receipt/payment/booking/outbox atomically.
6. `SUCCESS` trong hold cập nhật booking `CONFIRMED/PAID`; `FAILED/EXPIRED` giải phóng slot.
7. Callback success đến sau hold không hồi sinh booking: payment ghi nhận success/late, booking giữ `EXPIRED` hoặc `PAYMENT_FAILED` và tạo refund 100%.
8. Refund có aggregate riêng `PENDING/SUCCEEDED/FAILED`; không ghi đè lịch sử payment thành công.
9. Không log raw callback, signature, checkout URL hoặc dữ liệu payment nhạy cảm.

## Conversation và message

1. Một conversation active tối đa cho mỗi booking.
2. Chỉ user và expert của booking được truy cập.
3. Chat mở từ `startAt - 24h` đến `endAt + 24h`; đặt lịch trong cửa sổ thì mở ngay sau confirm. Booking hủy chỉ còn read-only.
4. Tin nhắn được persist trước khi xác nhận delivery; WebSocket disconnect không làm mất dữ liệu.
5. MVP chỉ cho gửi `TEXT`; schema giữ type attachment cho phase sau.
6. Không đưa content/attachment vào event notification; chỉ dùng IDs và preview đã redacted nếu policy cho phép.
7. Lịch sử dùng keyset và không trả message đã soft-delete.

## Consultation note

1. Một note active cho mỗi booking.
2. Chỉ expert đúng booking `CONFIRMED/COMPLETED` được tạo note; note khóa sau `endAt + 24h`.
3. Lưu observation/recommendation/recovery plan; không dùng hệ thống tự động tạo chẩn đoán.
4. User chỉ xem `recommendation/recoveryPlan` khi `visibleToUser=true`; `observation` không được trả cho user.

## Review

1. Chỉ user của booking `COMPLETED` được review expert trong booking đó.
2. Một review active/booking, rating nguyên `1..5`.
3. Không cho expert tự review hoặc review booking chưa hoàn tất.
4. Rating projection gửi sang Auth bằng outbox event sau review hợp lệ; khi soft-delete review phải phát projection mới có kiểm thử.

## Rule còn mở

- Payment provider đầu tiên, retry provider-specific và signature contract.
- Attachment type/size, moderation và retention chat.
- Consent cho expert xem dữ liệu Emotion.
- Ai được xem phần nào của consultation note; retention/export/erasure.
- Contract Auth đọc trạng thái/fee và event xử lý expert bị khóa.
