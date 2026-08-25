# Tín hiệu check-in hằng ngày

Migration V8 mở rộng nhật ký cảm xúc với ba chỉ số chủ quan tùy chọn theo thang 1-5:

- `energyLevel`: mức năng lượng, 1 là rất thấp và 5 là rất cao.
- `stressLevel`: mức căng thẳng, 1 là rất ít và 5 là rất nhiều.
- `sleepQuality`: cảm nhận về chất lượng giấc ngủ, 1 là rất kém và 5 là rất tốt.

Đây là tự đánh giá tại thời điểm check-in, không phải dữ liệu đo từ thiết bị và không thay thế Health
Connect/HealthKit. Ba field là nullable để giữ tương thích với nhật ký cũ và cho phép người dùng chỉ ghi
những gì họ muốn. Nhật ký vẫn bất biến và áp dụng cửa sổ soft-delete hiện hành.
