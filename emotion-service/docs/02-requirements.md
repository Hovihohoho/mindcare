# Yêu cầu Emotion Service

## Quy ước

- **Must**: cần cho MVP hoặc an toàn dữ liệu.
- **Should**: quan trọng nhưng có thể triển khai sau capability lõi.
- **Could**: mở rộng sau MVP.
- Trạng thái `Planned` nghĩa là chưa có bằng chứng triển khai trong repository.

## Yêu cầu chức năng

| ID | Nguồn | Mức | Yêu cầu | Tiêu chí chấp nhận tóm tắt | Trạng thái |
|---|---|---:|---|---|---|
| ES-FR-001 | UC 2.1 | Must | Ghi nhật ký cảm xúc | User đã xác thực tạo bản ghi với loại hợp lệ, ghi chú tùy chọn và thời điểm server; chỉ đọc được dữ liệu của mình | Implemented: create API |
| ES-FR-002 | UC 2.2 | Must | Xem lịch sử cảm xúc | Lọc theo khoảng thời gian, phân trang ổn định, không trả bản ghi đã xóa | Implemented |
| ES-FR-003 | UC 2.3 | Must | Xem xu hướng cảm xúc | Trả time-series liên tục theo ngày/tuần/tháng, timezone và mapping điểm được khai báo | Implemented |
| ES-FR-004 | UC 2.4 | Should | Ghi nhận trạng thái consent đồng bộ | Service chỉ nhận trạng thái/metadata cần thiết; hộp thoại quyền do mobile/native quản lý | Planned; schema chưa hỗ trợ |
| ES-FR-005 | UC 2.5 | Must | Đồng bộ health metrics | Validate loại/đơn vị/thời gian, nhận batch idempotent và lưu theo mốc ghi nhận | Planned |
| ES-FR-006 | UC 3.1 | Must | Lấy và nộp bài assessment | Câu hỏi đúng thứ tự; mọi câu trả lời thuộc đúng bài/phiên bản; chấm điểm deterministic | Implemented: published list/detail/submit |
| ES-FR-007 | UC 3.2 | Must | Xem kết quả assessment | Lưu tổng điểm, phân loại, chi tiết truy vết và khuyến nghị an toàn; user xem lịch sử của mình | Implemented: submit response, owned history and detail |
| ES-FR-008 | UC 3.3 | Should | Phân tích dữ liệu tổng hợp | Kết hợp cửa sổ cảm xúc 7 ngày, assessment gần nhất và health metrics 3-7 ngày; rule/version và lý do được lưu | Planned |
| ES-FR-009 | UC 3.4 | Should | Tạo cảnh báo chủ động | Khi rule vượt ngưỡng, tạo alert idempotent và phát notification intent; không gửi FCM trong service | Planned |
| ES-FR-010 | UC 5.6 | Should | Expert đọc bản tóm tắt được chia sẻ | Chỉ expert có quan hệ tư vấn và consent hợp lệ được đọc đúng phạm vi/thời hạn | Planned; cần contract Booking/Auth |
| ES-FR-011 | UC 6.2 | Must | Admin quản lý bộ assessment | CRUD/version/publish/archive; response scale do server sinh theo code; không làm thay đổi hồi tố kết quả cũ | Partial: Admin list/detail/create/update draft/create-next-version/publish/archive implemented; hard-delete is intentionally not exposed |
| ES-FR-012 | UC 3.5 | Could | Cung cấp tín hiệu để gợi ý chuyên gia | Phát mức rủi ro/nhóm nhu cầu tối thiểu; AI/Booking chịu trách nhiệm chọn và hiển thị chuyên gia | Planned |
| ES-FR-013 | Suy ra từ privacy | Must | Xóa mềm/export dữ liệu thuộc user | Có luồng được ủy quyền, audit và retention; chi tiết pháp lý cần xác nhận | Partial: journal soft delete implemented |

## Yêu cầu phi chức năng

### Bảo mật và riêng tư

| ID | Yêu cầu |
|---|---|
| ES-NFR-SEC-01 | Request ngoài public catalog phải có JWT hợp lệ qua Gateway; service kiểm tra claim/quyền cần thiết theo defense in depth. |
| ES-NFR-SEC-02 | RBAC hỗ trợ `ROLE_USER`, `ROLE_EXPERT`, `ROLE_ADMIN`; quyền đọc dữ liệu user còn cần ownership/consent, không chỉ role. |
| ES-NFR-SEC-03 | Kết nối ngoài hệ thống dùng HTTPS/TLS 1.3 theo yêu cầu triển khai. Kết nối nội bộ và DB cần chính sách TLS riêng theo môi trường. |
| ES-NFR-SEC-04 | Dữ liệu nhạy cảm cần mã hóa at rest; tài liệu gốc đề xuất AES-256 cho trường đặc biệt nhạy cảm. Cách quản lý khóa là open decision. |
| ES-NFR-SEC-05 | Chống SQL injection bằng parameter binding/JPA, validate input, encode output; CORS/rate limit chủ yếu tại Gateway. |
| ES-NFR-SEC-06 | Không log token, nội dung journal, answers detail hoặc giá trị metric chi tiết. Audit truy cập expert/admin phải không thể sửa tùy tiện. |
| ES-NFR-SEC-07 | Secret không nằm trong source/config mặc định dùng chung. Credential local hiện có trong `application.yml` phải được thay bằng biến môi trường an toàn. |

### Hiệu năng và khả năng mở rộng

- Tài liệu nguồn bị thiếu con số cho thời gian phản hồi API nghiệp vụ thông thường; cần đo baseline rồi chốt SLO p95/p99.
- Yêu cầu `< 2.5s` cho phản hồi AI đầy đủ hoặc bắt đầu stream `< 500ms` thuộc AI Service, không phải SLO trực tiếp của Emotion Service.
- API danh sách phải phân trang; trend query phải giới hạn khoảng thời gian và dùng index phù hợp.
- Batch ingest cần giới hạn số phần tử/payload và xử lý idempotent.
- HikariCP được cấu hình theo tài nguyên môi trường, không sao chép một cấu hình cho mọi service.
- Redis có thể cache assessment đã publish và system config; cache không phải nguồn sự thật.
- Phân tích nền và notification sử dụng RabbitMQ khi hạ tầng được bổ sung.

### Tin cậy và vận hành

- Migration phải deterministic, có backup/rollback playbook cho thay đổi phá vỡ.
- Event delivery giả định at-least-once; producer/consumer phải idempotent.
- Health/readiness probe phân biệt tình trạng tiến trình với dependency quan trọng.
- Metric/log/trace có correlation ID và không chứa PII/PHI.
- Backup, RPO, RTO, retention và disaster recovery chưa được tài liệu nguồn định lượng; phải chốt trước production.

### Tương thích và khả dụng

- REST JSON, UTF-8, version `/api/v1`, timestamp ISO-8601 UTC/offset.
- OpenAPI là contract có version; thay đổi breaking cần version mới hoặc kế hoạch deprecation.
- Error response ổn định, có machine-readable code và trace ID, không lộ stack trace.
- Message/cảnh báo viết bằng ngôn ngữ hỗ trợ, dễ hiểu, không kỳ thị và không đưa ra chẩn đoán.

## Giả định và điểm cần xác nhận

1. PostgreSQL là cơ sở dữ liệu nghiệp vụ và nguồn sự thật duy nhất của Emotion Service; không sử dụng database engine khác cho dữ liệu do service sở hữu.
2. Mobile chịu trách nhiệm gọi SDK Health Connect/HealthKit và chỉ gửi dữ liệu đã có consent.
3. Gateway cung cấp UUID qua verified header `X-User-Id` và claim role đơn qua `X-User-Role` với một trong `ROLE_USER`, `ROLE_EXPERT`, `ROLE_ADMIN`. Gateway phải xóa/ghi đè các header do client gửi và Emotion Service chỉ được nhận traffic qua đường mạng tin cậy. Service-to-service identity vẫn cần contract.
4. Booking Service cung cấp quyết định authorization khi expert xem dữ liệu; chưa có contract.
5. Bộ thang đo, scoring, threshold và nội dung khuyến nghị phải được chuyên gia chuyên môn phê duyệt trước production.
6. Chính sách crisis escalation, retention và consent vẫn chưa đủ chi tiết để triển khai production.
7. Trong giai đoạn test chưa giới hạn tần suất tạo journal. Trước production phải chốt rate limit chống spam tại Gateway và/hoặc service mà không làm mất request hợp lệ.
