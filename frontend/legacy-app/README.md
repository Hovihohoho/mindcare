# MindCare Frontend

Ứng dụng React duy nhất cho ba khu vực của MindCare.

## Chạy local

```powershell
npm install
npm run dev
```

Mặc định frontend gọi API Gateway tại `http://localhost:8079`. Có thể thay đổi bằng `VITE_API_URL`.

## Khu vực

- `/`: giao diện người dùng và đăng nhập/đăng ký chung.
- `/expert`: cổng làm việc của tài khoản `ROLE_EXPERT`.
- `/admin`: quản trị người dùng, chuyên gia và tài liệu AI dành cho `ROLE_ADMIN`.

Sau khi đăng nhập, ứng dụng tự chuyển tới khu vực tương ứng với vai trò tài khoản.

## Cấu trúc

```text
src/
├── auth/                  # Bảo vệ route theo vai trò
├── components/            # Component dùng chung
├── layouts/               # Layout theo khu vực
├── pages/
│   ├── user/              # Website người dùng
│   ├── expert/            # Portal chuyên gia
│   └── admin/             # Dashboard quản trị
├── services/              # Fetch/Axios client và session
├── App.jsx                # Chọn khu vực và lazy-load module
└── main.jsx               # BrowserRouter/basename dùng chung
```
