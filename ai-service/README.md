# MindCare AI Service

## Cấu hình Gemini

Tạo API key trong Google AI Studio rồi cập nhật file `.env` ở thư mục gốc:

```env
GEMINI_API_KEY=your-real-api-key
GEMINI_EMBEDDING_MODEL=gemini-embedding-2
GEMINI_CHAT_MODEL=gemini-3.1-flash-lite
GEMINI_FALLBACK_CHAT_MODEL=gemini-3.1-flash-lite
```

Khởi động đầy đủ AI Service:

```powershell
.\run-all.ps1 -RestartExisting
```

Không dùng `-SkipAi` khi muốn chat hoặc tạo embedding.

## Nạp dữ liệu cho AI

1. Đăng nhập bằng tài khoản `ROLE_ADMIN`.
2. Mở `/admin/ai-documents`.
3. Chọn **Thêm tài liệu** để nhập nội dung, hoặc **Nhập TXT/MD**.
4. Điền nguồn tham khảo và loại tài liệu.
5. Chỉ bật **Cho phép AI sử dụng** sau khi nội dung đã được kiểm duyệt.
6. Nhấn **Lưu và tạo embedding**. AI Service lưu văn bản và gọi Gemini Embedding.
7. Khi đổi model embedding hoặc cần làm mới dữ liệu, dùng **Reindex tất cả**.

File nhập hỗ trợ `.txt` và `.md`, tối đa 2 MB ở giao diện. Không nạp hồ sơ cá nhân,
thông tin nhận dạng, hội thoại riêng tư hoặc tài liệu chưa xác minh. Nên chia tài
liệu dài thành các chủ đề độc lập để kết quả tìm kiếm RAG chính xác hơn.

## API quản trị

- `GET /api/ai/documents`: danh sách tài liệu.
- `POST /api/ai/documents`: lưu tài liệu và tạo embedding.
- `PUT /api/ai/documents/{id}`: cập nhật và tạo lại embedding.
- `POST /api/ai/documents/{id}/reindex`: tạo lại một embedding.
- `POST /api/ai/documents/reindex-all`: tạo lại embedding của mọi tài liệu active.
- `DELETE /api/ai/documents/{id}`: xóa tài liệu.
- `POST /api/ai/chat`: tìm kiếm vector và tạo câu trả lời có grounding.

AI Service chỉ nên được truy cập qua API Gateway. Không công khai trực tiếp cổng
`8084`, vì Gateway chịu trách nhiệm xác minh JWT và gắn danh tính người dùng.
