# Yêu cầu Booking Service

## Yêu cầu chức năng

| ID | Nguồn | Mức | Yêu cầu | Tiêu chí chấp nhận | Trạng thái |
|---|---|---:|---|---|---|
| BS-FR-001 | UC 5.1 | External | Tìm kiếm/xem expert profile | Auth Service sở hữu và phát hành catalog contract; Booking không persist profile | Auth scope |
| BS-FR-002 | UC 5.1 | Must | Xem slot trống theo expert | `expertUserId` đến từ catalog Auth; chỉ slot tương lai `AVAILABLE` | Implemented |
| BS-FR-003 | UC 1.2/5.9 | External | Expert quản lý hồ sơ chuyên môn | Thực hiện hoàn toàn tại Auth Service | Auth scope |
| BS-FR-004 | UC 6.1 | External | Admin duyệt hồ sơ expert | Thực hiện hoàn toàn tại Auth Service; Auth phát lifecycle event/contract | Auth scope |
| BS-FR-005 | UC 5.4 | Must | Expert quản lý schedule | Không trùng slot active; chỉ sửa/xóa slot chưa bị giữ/đặt | Implemented |
| BS-FR-006 | UC 5.2 | Must | User instant booking | Giữ slot 15 phút, tạo payment ngay, callback thành công tự confirm, không double-book | Controller implemented; provider pending; local bypass available |
| BS-FR-007 | UC 5.3 | Must | User xem/hủy booking | `>=24h` hủy/hoàn 100%; `[2h,24h)` chờ expert tối đa 60 phút; `<2h` từ chối | Implemented |
| BS-FR-008 | UC 5.5 | Must | Expert xử lý lịch | Không accept/reject booking; expert duyệt yêu cầu hủy, tự hủy, complete và ghi nhận user no-show | Implemented |
| BS-FR-009 | UC 5.11 | Must | Thanh toán online | Idempotency, amount server-owned, callback verified/deduplicate, late success tự tạo full refund | Core implemented; provider adapter pending |
| BS-FR-010 | UC 5.8 | Should | Chat realtime với expert | Chỉ participant của booking hợp lệ; persist history; WebSocket delivery | REST implemented; WebSocket pending |
| BS-FR-011 | UC 5.7 | Must | Consultation note | Một note active/booking; expert đúng booking; user chỉ xem phần được phép | Implemented |
| BS-FR-012 | UC 5.10 | Must | Review expert | Booking completed, đúng user/expert, một review/booking, rating 1-5 | Implemented |
| BS-FR-013 | UC 5.6 | Should | Cấp quyền xem dữ liệu Emotion | Booking Service xác nhận quan hệ/consent; Emotion Service sở hữu dữ liệu | Planned; cần consent contract |
| BS-FR-014 | UC 6.4 | Should | Thống kê booking | Dữ liệu tổng hợp, không lộ nội dung note/chat | Planned |

## Yêu cầu phi chức năng

### Bảo mật

- JWT được xác minh tại Gateway; service vẫn kiểm tra role/ownership.
- Role: `ROLE_USER`, `ROLE_EXPERT`, `ROLE_ADMIN`.
- Gateway phải xóa/ghi đè verified identity headers từ client ngoài.
- Payment callback không dùng user JWT; bắt buộc provider signature, replay protection và allowlist khi khả dụng.
- Không trả entity trực tiếp; không mass assignment các field status/price/owner.
- Nội dung consultation/chat là dữ liệu nhạy cảm, cần encryption-at-rest/platform key decision trước production.

### Nhất quán

- Booking/schedule transition trong một transaction.
- Optimistic version trên aggregate thay đổi thường xuyên.
- Tạo slot dùng PostgreSQL transaction-level advisory lock theo `expertUserId` để chống insert overlap đồng thời.
- Payment callback và outbox consumer idempotent.

### Hiệu năng

- API thông thường đặt mục tiêu p95 dưới 500 ms sau khi có workload/SLO chính thức.
- History dùng keyset pagination, không dùng offset sâu.
- HikariCP cấu hình qua môi trường.
- Cache kết quả đọc expert từ Auth chỉ là tối ưu; Auth vẫn là nguồn sự thật của profile.

### Vận hành

- Correlation/trace ID xuyên REST/event.
- Log có cấu trúc, redaction dữ liệu nhạy cảm.
- Migration forward-only, backup/restore và RPO/RTO cần chốt trước production.
