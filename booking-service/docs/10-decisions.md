# Nhật ký quyết định Booking Service

## DEC-001 - Ranh giới service

- Trạng thái: **Accepted**
- Ngày: 2026-07-25
- Quyết định: Auth Service sở hữu account/JWT và toàn bộ expert professional profile/catalog. Booking Service sở hữu schedule, booking, payment, expert chat, review và consultation note.
- Hệ quả: Booking chỉ lưu external `expert_user_id`, đọc eligibility/fee/profile qua contract Auth và không có FK xuyên schema. Review gốc thuộc Booking; rating projection có thể phát sang Auth.

## DEC-002 - PostgreSQL là nguồn sự thật duy nhất

- Trạng thái: **Accepted**
- Ngày: 2026-07-25
- Quyết định: toàn bộ dữ liệu Booking Service, kể cả message history, dùng PostgreSQL `booking_schema`. WebSocket là transport; Redis nếu có chỉ cache/presence.
- Lý do: bản đề tài kiến trúc mới hơn xác định PostgreSQL là database nghiệp vụ duy nhất; đơn giản hóa consistency booking/payment/chat.
- Mâu thuẫn đã xử lý: tài liệu NFR cũ đề xuất DynamoDB cho message; baseline này thay thế đề xuất đó.

## DEC-003 - UUID/thời gian/audit do application quản lý

- Trạng thái: **Accepted**
- Ngày: 2026-07-25
- Quyết định: UUID sinh bằng Hibernate `GenerationType.UUID`; migration không UUID default. Database dùng `TIMESTAMPTZ`, Java dùng `OffsetDateTime`. JPA Auditing quản lý `created_at`/`updated_at`, không trigger/default.

## DEC-004 - Booking và schedule lifecycle

- Trạng thái: **Accepted baseline**
- Ngày: 2026-07-25
- Quyết định: Instant Booking; slot `AVAILABLE -> HELD -> BOOKED`, booking `PAYMENT_PENDING -> CONFIRMED` hoặc `PAYMENT_FAILED/EXPIRED`. Không có expert accept/reject.
- Consistency: PostgreSQL transaction-level advisory lock theo `expertUserId` chống schedule overlap; slot lock + partial unique index chống double-book; optimistic version trên mutable aggregate.
- Cancellation/payment ordering được chốt tại DEC-011.

## DEC-005 - Money và payment callback

- Trạng thái: **Accepted baseline**, provider-specific contract còn provisional
- Ngày: 2026-07-25
- Quyết định: `BigDecimal`/`NUMERIC(12,2)` + currency; booking snapshot giá. Payment create bắt buộc idempotency; callback bắt buộc signature verification và receipt dedup.
- Không lưu card/bank secret hoặc raw webhook mặc định.

## DEC-006 - REST, WebSocket và outbox

- Trạng thái: **Accepted baseline**
- Ngày: 2026-07-25
- Quyết định: REST cho schedule/booking/payment/review/note; catalog/profile API thuộc Auth. WebSocket cho expert chat realtime; PostgreSQL lưu message. RabbitMQ/outbox cho notification/integration.
- Event không chứa content/note/payment secret.

## DEC-007 - Identity/RBAC

- Trạng thái: **Provisional**
- Ngày: 2026-07-25
- Baseline: Gateway xác minh JWT, chuyển verified `X-User-Id`/`X-User-Role`; service tạo SecurityContext và kiểm tra ownership.
- Chưa chốt: service-to-service identity, WebSocket token forwarding và bảo vệ internal consent endpoint.

## DEC-008 - Port local và Gateway route

- Trạng thái: **Accepted cho local booking-service**
- Ngày: 2026-07-25
- Quyết định: Booking Service dùng port `8083`; Emotion Service hiện dùng `8082`.
- Hiện trạng: `api-gateway/application.yml` đang route expert vào `8082` và emotion vào `8083`, ngược trạng thái thực tế. Không sửa ngoài phạm vi booking-service; Gateway cần task riêng trước end-to-end.

## DEC-009 - V1 schema baseline

- Trạng thái: **Accepted trước khi áp dụng môi trường dùng chung**
- Ngày: 2026-07-25
- Quyết định: V1 tạo 10 bảng gồm schedule/booking/chat/review/note/payment/refund/receipt/outbox; không tạo `expert_profiles`.
- Sau khi V1 chạy ở môi trường dùng chung, không sửa; mọi thay đổi bằng V2+.

## DEC-010 - Expert profile thuộc Auth Service

- Trạng thái: **Accepted**
- Ngày: 2026-07-25
- Quyết định: expert kế thừa identity của user và toàn bộ hồ sơ chuyên môn, duyệt, trạng thái, catalog cùng giá tư vấn nằm tại Auth Service. Booking chỉ tham chiếu `expert_user_id`.
- Dữ liệu lịch sử: `bookings` snapshot `price`/`currency`; các bảng schedule/review/note lưu external expert ID để authorization và truy vấn.
- Tích hợp: Auth cung cấp contract đọc expert eligibility/profile/fee và phát lifecycle event; Booking phát rating projection từ review.
- Migration: V1 Booking chưa chạy ở môi trường dùng chung nên được sửa trực tiếp, không tạo V2.

## DEC-011 - Instant Booking, cancellation, refund và consultation window

- Trạng thái: **Accepted**
- Ngày: 2026-07-25
- Instant Booking: expert mở slot là chấp thuận nhận lịch; checkout hold 15 phút, payment success tự chuyển booking `CONFIRMED`.
- Cancellation: user hủy `>=24h` được refund 100%; `[2h,24h)` chờ expert tối đa 60 phút và trước mốc 2 giờ; timeout tự từ chối; `<2h` không được hủy.
- Expert hủy tạo refund 100%. Refund là aggregate riêng `PENDING/SUCCEEDED/FAILED`.
- Late callback: không hồi sinh booking; ghi payment late success và tự tạo full refund.
- Chat: write từ 24 giờ trước đến 24 giờ sau slot, ngoài cửa sổ read-only; MVP chỉ gửi text.
- Complete: expert đúng booking, payment paid và đã qua `endAt`; user no-show sau grace 15 phút.
- Các duration là cấu hình, không hard-code.

## DEC-012 - Local expert adapter và payment bypass

- Trạng thái: **Accepted cho local/test**
- Ngày: 2026-07-25
- Quyết định: `BOOKING_AUTH_ADAPTER_MODE=local` cung cấp catalog/profile expert giả định danh để test độc lập khi Auth contract chưa phát hành. `BOOKING_PAYMENT_REQUIRED=false` cho phép booking được confirm ngay với `paymentStatus=UNPAID`.
- An toàn: payment mặc định là `true`; production không được bật local adapter hoặc payment bypass. Khi payment bật, contract checkout cũ và payment provider fail-closed vẫn giữ nguyên.
- Phạm vi: không tạo expert profile trong PostgreSQL Booking và không thay đổi migration V1.

## Quyết định mở

| ID | Câu hỏi | Chặn |
|---|---|---|
| OPEN-03 | Provider payment đầu tiên, signature/IPN contract? | Payment implementation |
| OPEN-04 | Attachment/moderation/retention chat? | WebSocket attachment/production |
| OPEN-05 | Consent scope/source cho expert xem Emotion data? | UC 5.6 |
| OPEN-06 | Consultation note visibility, encryption, retention/export? | Privacy production |
| OPEN-07 | Service identity và Gateway-to-service trust? | Internal/production security |
| OPEN-08 | SLO/RPO/RTO và workload? | Capacity/operations |
| OPEN-09 | Auth API/event schema cho eligibility, fee/currency và expert lifecycle? | Schedule/booking implementation |
