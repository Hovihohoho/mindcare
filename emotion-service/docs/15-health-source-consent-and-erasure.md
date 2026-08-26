# Quyền đồng bộ và xóa dữ liệu nguồn sức khỏe

## Mục tiêu

Quyền đọc trong Health Connect trên thiết bị và quyền cho phép MindCare tiếp tục nhận dữ liệu là hai trạng
thái khác nhau. Thu hồi quyền trên Android ngăn ứng dụng đọc dữ liệu mới nhưng không tự xóa dữ liệu đã gửi
lên MindCare. Capability này bổ sung quyền kiểm soát phía máy chủ.

## API

- `GET /api/v1/health-metrics/sources/{sourceType}`: số bản ghi, khoảng thời gian, số lượng theo metric và trạng thái đồng bộ.
- `POST /api/v1/health-metrics/sources/{sourceType}/enable`: cho phép nguồn đồng bộ trở lại.
- `DELETE /api/v1/health-metrics/sources/{sourceType}/data`: thu hồi quyền nhận dữ liệu và xóa vật lý toàn bộ record cùng metadata idempotency của nguồn.

Tất cả endpoint lấy `userId` từ SecurityContext. Sau khi xóa, mọi batch mới của nguồn bị từ chối bằng
`HEALTH_SOURCE_CONSENT_REQUIRED` cho tới khi người dùng chủ động kết nối lại.

## Migration

V10 tạo `health_source_consents` trong `emotion_schema`. V7 dành cho mở rộng Health Connect, V8 dành cho
kế hoạch tự chăm sóc và V9 dành cho tín hiệu check-in hằng ngày.

## Giới hạn

Việc xóa chỉ áp dụng dữ liệu thuộc Emotion Service. Export/xóa toàn tài khoản trên mọi service vẫn cần một
privacy workflow điều phối riêng.
