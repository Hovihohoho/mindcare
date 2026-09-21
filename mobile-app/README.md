# MindCare Mobile

Ứng dụng Expo SDK 54/React Native một cột cho Android và iOS, chuyển ngôn ngữ thiết kế từ `mindcare-frontend/web-app` sang trải nghiệm mobile thay vì thu nhỏ bố cục desktop. Metro chạy mặc định tại cổng `8086` để không xung đột với các service khác.

## Chạy project

```bash
npm install
npm start
```

Sau đó nhấn `a` để mở Android, hoặc quét QR bằng Expo Go trên iOS. Có thể dùng `npm run web` để kiểm tra nhanh responsive trên trình duyệt. Tất cả script `start`, `android`, `ios` và `web` đều dùng port `8086`.

## Kiểm tra chất lượng

```bash
npm run typecheck
npm run lint
npx expo export --platform web
```

## Cấu trúc chính

- `src/app/(auth)`: luồng đăng nhập/đăng ký và route guard trước khi vào ứng dụng.
- `src/features/auth`: UI form, auth provider và trạng thái khôi phục phiên.
- `src/features/emotion`: biểu đồ tuần, bộ chọn cảm xúc, type và hook dữ liệu nhật ký.
- `src/services/api`: API client dùng `EXPO_PUBLIC_API_URL`, mặc định kết nối gateway cổng `8079`.
- `src/services/auth`: auth API thật và adapter lưu phiên an toàn.
- `src/services/emotion`: API thật cho tạo nhật ký, lịch sử và xu hướng cảm xúc.
- `src/app/(tabs)`: Expo Router và 5 bottom tabs.
- `src/screens`: Nhật ký, Đánh giá, AI và Cài đặt.
- `src/components`: search, card states, bottom sheet và UI dùng chung.
- `src/theme/tokens.ts`: màu, font, spacing, radius và shadow lấy từ web MindCare.

## Kiểm thử đăng nhập

Auth mobile gọi backend thật qua API Gateway. Đăng ký yêu cầu mật khẩu 8–72 ký tự, có chữ và số; sau đó người dùng nhập mã xác thực 6 chữ số gửi qua email rồi mới đăng nhập. Phiên được lưu bằng `expo-secure-store` trên iOS/Android, vì vậy đóng rồi mở lại app sẽ vào thẳng màn Nhật ký nếu token vẫn hợp lệ. Trên web, project dùng `localStorage` làm fallback.

Sao chép `.env.example` thành `.env` và đặt `EXPO_PUBLIC_API_URL` thành địa chỉ gateway mà điện thoại truy cập được, ví dụ `http://192.168.1.10:8079`. Trong Expo Go development, nếu biến này bị bỏ trống, app tự lấy IP của Metro và dùng cổng `8079`. Không dùng `localhost` trên điện thoại thật.

Nút **Đăng xuất** nằm cuối danh sách trong tab **Cài đặt**, gọi `/api/auth/logout` rồi xóa phiên cục bộ.

## Kết nối backend

Màn Nhật ký gọi trực tiếp các endpoint `/api/v1/emotion-journals` và `/api/v1/emotion-trends` qua API Gateway. Bearer token lấy từ phiên đăng nhập trong SecureStore. Các API còn lại được tổ chức trong `src/services` và theo tính năng trong `src/features`; adapter mock cũ đã được loại bỏ.
