# Kế hoạch tự chăm sóc

## Phạm vi nghiệp vụ

Kế hoạch tự chăm sóc giúp người dùng chọn một mục tiêu nhỏ, thực hiện các hoạt động hằng ngày và theo dõi
tiến độ trong tuần. Đây là công cụ xây dựng thói quen, không phải chẩn đoán hoặc kế hoạch điều trị.

Mỗi người dùng có tối đa một kế hoạch hiện hành, gồm một mục tiêu và từ một đến năm hoạt động. Thay đổi
kế hoạch là full replacement. Mỗi hoạt động chỉ có một completion mỗi ngày; người dùng chỉ được cập nhật
completion trong tuần hiện tại, tính từ thứ Hai.

## API đã triển khai

- `GET /api/v1/self-care-plan`: lấy kế hoạch và tiến độ tuần.
- `PUT /api/v1/self-care-plan`: tạo hoặc thay thế kế hoạch.
- `POST /api/v1/self-care-plan/activities/{id}/completions`: đánh dấu hoàn thành.
- `DELETE /api/v1/self-care-plan/activities/{id}/completions/{date}`: bỏ đánh dấu.

Các endpoint chỉ dành cho `ROLE_USER`; chủ sở hữu luôn lấy từ SecurityContext.

## Dữ liệu V7

V7 bổ sung `self_care_plans`, `self_care_activities` và `self_care_completions` trong `emotion_schema`.
Mỗi user có một plan; activity code và thứ tự là duy nhất trong plan; completion là duy nhất theo
`(activity_id, completed_on)`. `user_id` là tham chiếu ngoài, không có foreign key xuyên service.

## Quyết định

- Dữ liệu thuộc Emotion Service và PostgreSQL là nguồn sự thật duy nhất.
- Mục tiêu là enum đóng: giảm căng thẳng, cải thiện giấc ngủ, quản lý lo âu và xây dựng cân bằng.
- Không lưu chẩn đoán, phác đồ hoặc cam kết hiệu quả y khoa trong kế hoạch.
