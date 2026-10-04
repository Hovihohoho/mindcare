# Kịch bản demo MindCare

## 1. Khởi động

Từ thư mục gốc của dự án, chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-all.ps1 -SeedDemo -UseMailpit -RestartExisting
```

Các địa chỉ dùng khi demo:

- Giao diện: http://localhost:5173
- API Gateway: http://localhost:8079
- Mailpit: http://localhost:8025

Tài khoản người dùng chính:

- Email: `user1@mindcare.local`
- Mật khẩu: `MindCare@123`

Sau khi hệ thống khởi động, kiểm tra nhanh toàn bộ luồng đọc dữ liệu:

```powershell
powershell -ExecutionPolicy Bypass -File .\demo-smoke.ps1
```

## 2. Nội dung dữ liệu mẫu

Dữ liệu của `user1` được tạo thành một câu chuyện nhất quán để trình bày:

- Nhật ký cảm xúc trong nhiều ngày, đủ để hiển thị lịch sử và biểu đồ xu hướng.
- Các lần làm bài đánh giá tâm lý, gồm một kết quả PHQ-9 gần đây ở mức nhẹ.
- Dữ liệu 7 ngày cho giấc ngủ, số bước, nhịp tim nghỉ, SpO2 và vận động.
- Các đánh giá benchmark sức khỏe, trong đó có cảnh báo cần chú ý.
- Kế hoạch tự chăm sóc cùng các hoạt động đã hoàn thành trong tuần.
- Bốn cuộc trò chuyện AI có sẵn, mỗi cuộc có lịch sử tin nhắn hoàn chỉnh.
- Thông báo thường và thông báo cảnh báo rủi ro tự động.

Seed có thể chạy lại nhiều lần. Các bản ghi cố định dùng khóa xung đột để không nhân đôi dữ liệu.

## 3. Kịch bản trình bày 7–10 phút

1. **Đăng nhập và trang tổng quan** — giới thiệu người dùng mẫu và các chỉ số tổng quan.
2. **Nhật ký cảm xúc** — mở lịch sử, chỉ ra dữ liệu nhiều ngày và biểu đồ xu hướng.
3. **Bài đánh giá** — mở lịch sử PHQ-9, giải thích kết quả gần đây và khuyến nghị đi kèm.
4. **Dữ liệu sức khỏe** — lần lượt xem năm nhóm dữ liệu; nhấn mạnh cảnh báo được sinh từ benchmark.
5. **Kế hoạch tự chăm sóc** — cho thấy tiến độ hoạt động trong tuần và đề xuất dựa trên dữ liệu.
6. **Trò chuyện với AI** — mở một cuộc trò chuyện đã lưu để demo ổn định. Chỉ gửi tin nhắn trực tiếp khi khóa Gemini đang hoạt động.
7. **Thông báo tự động** — mở biểu tượng chuông hoặc trang thông báo, chỉ ra cảnh báo rủi ro được tạo tự động sau bước phân tích.

Khi giải thích cảnh báo, nên nói rõ hệ thống hỗ trợ phát hiện sớm và khuyến nghị tìm trợ giúp; nó không chẩn đoán hay thay thế chuyên gia y tế.

## 4. Xử lý nhanh trước buổi demo

- Nếu báo cổng `8079`, `8083`, `8084` hoặc `5173` đang được dùng, thêm `-RestartExisting` như lệnh khởi động ở trên.
- Nếu màn hình thiếu dữ liệu, chạy lại lệnh với `-SeedDemo`, đợi các service báo sẵn sàng rồi chạy `demo-smoke.ps1`.
- Nếu AI trực tiếp không trả lời, vẫn dùng các cuộc trò chuyện đã seed để trình bày lịch sử. Kiểm tra cấu hình Gemini trong `.env` sau buổi demo.
- Nếu email cần kiểm tra, mở Mailpit tại http://localhost:8025; chế độ demo không gửi email thật.
