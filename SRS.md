# ĐẶC TẢ YÊU CẦU PHẦN MỀM (SOFTWARE REQUIREMENTS SPECIFICATION - SRS)
## DỰ ÁN: NỀN TẢNG THEO DÕI VÀ HỖ TRỢ CHĂM SÓC SỨC KHỎE TINH THẦN MINDCARE

---

| Thông tin dự án | Chi tiết |
|---|---|
| **Tên sản phẩm** | **MindCare - Mental Health & Self-Care Platform** |
| **Mã dự án** | `MINDCARE-MONOREPO` |
| **Tiêu chuẩn tài liệu** | IEEE Std 830-1998 / ISO/IEC/IEEE 29148:2018 |
| **Phiên bản tài liệu** | 2.0.0 (Cập nhật chuẩn hóa nghiệp vụ Self-Care & AI RAG) |
| **Ngày ban hành** | 27/08/2026 |
| **Trạng thái** | Hoàn thiện đặc tả kiến trúc và nghiệp vụ hiện hành |

---

## MỤC LỤC
1. [GIỚI THIỆU (INTRODUCTION)](#1-giới-thiệu-introduction)
   - 1.1 [Mục đích tài liệu](#11-mục-đích-tài-liệu)
   - 1.2 [Phạm vi hệ thống (System Scope)](#12-phạm-vi-hệ-thống-system-scope)
   - 1.3 [Định nghĩa, từ viết tắt và thuật ngữ](#13-định-nghĩa-từ-viết-tắt-và-thuật-ngữ)
   - 1.4 [Tài liệu tham khảo](#14-tài-liệu-tham-khảo)
   - 1.5 [Tổng quan tài liệu](#15-tổng-quan-tài-liệu)
2. [MÔ TẢ TỔNG QUAN HỆ THỐNG (OVERALL DESCRIPTION)](#2-mô-tả-tổng-quan-hệ-thống-overall-description)
   - 2.1 [Bối cảnh sản phẩm & Sơ đồ kiến trúc logic](#21-bối-cảnh-sản-phẩm--sơ-đồ-kiến-trúc-logic)
   - 2.2 [Các phân hệ và chức năng tổng thể](#22-các-phân-hệ-và-chức-năng-tổng-thể)
   - 2.3 [Đối tượng người dùng & Phân quyền (User Classes & Actors)](#23-đối-tượng-người-dùng--phân-quyền-user-classes--actors)
   - 2.4 [Môi trường vận hành (Operating Environment)](#24-môi-trường-vận-hành-operating-environment)
   - 2.5 [Ràng buộc thiết kế và triển khai](#25-ràng-buộc-thiết-kế-và-triển-khai)
   - 2.6 [Giả định và sự phụ thuộc](#26-giả-định-và-sự-phụ-thuộc)
3. [YÊU CẦU GIAO DIỆN HỆ THỐNG (EXTERNAL INTERFACE REQUIREMENTS)](#3-yêu-cầu-giao-diện-hệ-thống-external-interface-requirements)
   - 3.1 [Giao diện người dùng (User Interfaces)](#31-giao-diện-người-dùng-user-interfaces)
   - 3.2 [Giao diện phần cứng (Hardware Interfaces)](#32-giao-diện-phần-cứng-hardware-interfaces)
   - 3.3 [Giao diện phần mềm (Software Interfaces)](#33-giao-diện-phần-mềm-software-interfaces)
   - 3.4 [Giao diện truyền thông và giao thức (Communication Interfaces)](#34-giao-diện-truyền-thông-và-giao-thức-communication-interfaces)
4. [YÊU CẦU CHỨC NĂNG CHI TIẾT (FUNCTIONAL REQUIREMENTS)](#4-yêu-cầu-chức-năng-chi-tiết-functional-requirements)
   - 4.1 [Phân hệ Xác thực & Hồ sơ cá nhân (Authentication & Profile)](#41-phân-hệ-xác-thực--hồ-sơ-cá-nhân-authentication--profile)
   - 4.2 [Phân hệ Quyền riêng tư & Quản lý Dữ liệu cá nhân (Privacy & Data Rights)](#42-phân-hệ-quyền-riêng-tư--quản-lý-dữ-liệu-cá-nhân-privacy--data-rights)
   - 4.3 [Phân hệ Nhật ký Cảm xúc & Phân tích Xu hướng (Emotion Journal & Analytics)](#43-phân-hệ-nhật-ký-cảm-xúc--phân-tích-xu-hướng-emotion-journal--analytics)
   - 4.4 [Phân hệ Đánh giá Tâm lý Chuẩn hóa (Psychological Assessments)](#44-phân-hệ-đánh-giá-tâm-lý-chuẩn-hóa-psychological-assessments)
   - 4.5 [Phân hệ Đồng bộ Dữ liệu Sức khỏe (Health Metrics Ingestion)](#45-phân-hệ-đồng-bộ-dữ-liệu-sức-khỏe-health-metrics-ingestion)
   - 4.6 [Phân hệ Trợ lý AI & Tra cứu Tri thức RAG (AI Assistant & RAG)](#46-phân-hệ-trợ-lý-ai--tra-cứu-tri-thức-rag-ai-assistant--rag)
   - 4.7 [Phân hệ Kế hoạch Tự chăm sóc & Bookmarks (Self-Care Plan & Bookmarks)](#47-phân-hệ-kế-hoạch-tự-chăm-sóc--bookmarks-self-care-plan--bookmarks)
   - 4.8 [Phân hệ Nhắc nhở & Thiết bị Nhận thông báo (Reminders & Push Devices)](#48-phân-hệ-nhắc-nhở--thiết-bị-nhận-thông-báo-reminders--push-devices)
   - 4.9 [Phân hệ Quản trị Hệ thống (Admin Management)](#49-phân-hệ-quản-trị-hệ-thống-admin-management)
5. [YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)](#5-yêu-cầu-phi-chức-năng-non-functional-requirements)
   - 5.1 [Bảo mật và Kiểm soát danh tính (Security Requirements)](#51-bảo-mật-và-kiểm-soát-danh-tính-security-requirements)
   - 5.2 [Hiệu năng và Giới hạn phản hồi (Performance Requirements)](#52-hiệu-năng-và-giới-hạn-phản-hồi-performance-requirements)
   - 5.3 [Tính sẵn sàng và Độ tin cậy (Availability & Reliability)](#53-tính-sẵn-sàng-và-độ-tin-cậy-availability--reliability)
   - 5.4 [Khả năng mở rộng và Bảo trì (Scalability & Maintainability)](#54-khả-năng-mở-rộng-và-bảo-trì-scalability--maintainability)
   - 5.5 [An toàn tâm lý và Chuẩn mực y tế (Medical Disclaimer & Ethical Guardrails)](#55-an-toàn-tâm-lý-và-chuẩn-mực-y-tế-medical-disclaimer--ethical-guardrails)
6. [MÔ HÌNH CƠ SỞ DỮ LIỆU & QUY ƯỚC LƯU TRỮ](#6-mô-hình-cơ-sở-dữ-liệu--quy-ước-lưu-trữ)
   - 6.1 [Chiến lược phân vùng Schema](#61-chiến-lược-phân-vùng-schema)
   - 6.2 [Danh mục bảng dữ liệu chi tiết](#62-danh-mục-bảng-dữ-liệu-chi-tiết)
   - 6.3 [Ma trận phân quyền dữ liệu (RBAC Matrix)](#63-ma-trận-phân-quyền-dữ-liệu-rbac-matrix)
7. [MA TRẬN TRUY VẾT YÊU CẦU (TRACEABILITY MATRIX)](#7-ma-trận-truy-vết-yêu-cầu-traceability-matrix)

---

## 1. GIỚI THIỆU (INTRODUCTION)

### 1.1 Mục đích tài liệu
Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này mô tả chi tiết và toàn diện các yêu cầu chức năng (Functional Requirements), yêu cầu phi chức năng (Non-Functional Requirements), kiến trúc kỹ thuật, mô hình dữ liệu và các giao diện bên ngoài của nền tảng **MindCare**. 
Tài liệu phục vụ làm cơ sở đối chiếu kỹ thuật cho:
- Đội ngũ phát triển Backend, Frontend Web, Mobile App.
- Kỹ sư đảm bảo chất lượng (QA/QC) xây dựng kịch bản kiểm thử tích hợp và kiểm thử hồi quy.
- Giảng viên/Hội đồng nghiệm thu đồ án tốt nghiệp và các bên liên quan (Stakeholders) nắm bắt chính xác phạm vi dự án.

### 1.2 Phạm vi hệ thống (System Scope)

#### 1.2.1 Phạm vi hỗ trợ (In Scope)
MindCare là giải pháp công nghệ kỹ thuật số định hướng **theo dõi và tự chăm sóc sức khỏe tinh thần (Self-care & Mental Health Tracking)**, hướng tới người dùng cá nhân (đặc biệt là học sinh, sinh viên và người trẻ tuổi):
1. **Quản lý danh tính và tài khoản an toàn**: Đăng ký, đăng nhập, bảo vệ đa yếu tố bằng mã OTP 6 chữ số gửi qua email SMTP thật/Mailpit, quản lý hồ sơ, ảnh đại diện và tùy chọn cá nhân.
2. **Quyền riêng tư dữ liệu (Data Rights & Privacy Controls)**: Người dùng có toàn quyền kết xuất dữ liệu cá nhân dạng máy đọc (JSON Data Export) và kích hoạt quy trình xóa sổ vĩnh viễn tài khoản cùng toàn bộ dữ liệu liên quan (Right to be Forgotten - Permanent Erasure Cascade).
3. **Nhật ký cảm xúc hằng ngày (Emotion Journaling)**: Cho phép ghi nhận trạng thái cảm xúc, thang năng lượng, mức căng thẳng, chất lượng giấc ngủ kèm ghi chú phản chiếu; cơ chế cửa sổ xóa mềm (soft-delete) 15 phút.
4. **Phân tích xu hướng tâm trạng (Emotion Analytics & Trends)**: Trực quan hóa dữ liệu chuỗi thời gian theo các chu kỳ Ngày / Tuần / Tháng, hỗ trợ múi giờ IANA, chuẩn hóa điểm số đánh giá.
5. **Sàng lọc tâm lý chuẩn hóa (Standardized Psychological Assessments)**: Cung cấp các bộ trắc nghiệm quốc tế (PHQ-9, GAD-7, WHO-5, PSS-10) với quy trình versioning bất biến, chấm điểm tự động từ phía server, phân loại nguy cơ và đưa ra các chỉ dẫn an toàn.
6. **Tích hợp tín hiệu sức khỏe thiết bị (Health Signals Ingestion)**: Đồng bộ hai chiều từ Google Health Connect (Android) và Apple HealthKit (iOS) đối với các chỉ số giấc ngủ, số bước chân, nhịp tim nghỉ, buổi vận động.
7. **Trợ lý AI hướng dẫn dựa trên tri thức kiểm duyệt (AI Assistant with Grounded RAG)**: Tích hợp Google Gemini (gemini-embedding-2, gemini-3.1-flash-lite), cơ chế tìm kiếm lai (Hybrid Search: Vector HNSW + Full-text GIN) hợp nhất bằng Reciprocal Rank Fusion (RRF), kèm phễu an toàn khủng hoảng (Crisis Safety Gate) phân loại rủi ro tự hại và định tuyến ứng phó khẩn cấp.
8. **Kế hoạch tự chăm sóc cá nhân hóa (Self-Care Plans)**: Thiết lập mục tiêu vi mô hàng tuần và ghi nhận tiến độ hoàn thành bài tập thực hành (hít thở, thiền định, thể dục).
9. **Nhắc nhở và Thông báo chủ động (Reminders & Notifications)**: Cấu hình thông báo theo lịch trình người dùng, hỗ trợ WebPush/FCM và WebSocket trực tiếp.

#### 1.2.2 Ranh giới loại trừ (Out of Scope - Bắt buộc tuân thủ theo BUSINESS_SCOPE.md)
Hệ thống **tuyệt đối không** hỗ trợ và không cam kết các tính năng sau:
- **Không có tài khoản chuyên gia (Expert accounts)**, không có quy trình xác minh chứng chỉ hành nghề, không có trang hồ sơ chuyên gia công khai (Toàn bộ mã nguồn cũ liên quan đến Expert Marketplace và Booking Schema đã được dọn sạch khỏi cơ sở dữ liệu).
- **Không có quy trình đặt lịch tư vấn (Appointment scheduling)**, không có phòng tư vấn 1-1 giữa chuyên gia và người dùng, không có ghi chú bệnh án lâm sàng (Clinical notes).
- **Không có tính năng thanh toán tư vấn**, cổng thanh toán dịch vụ lâm sàng, hóa đơn hoặc bảo hiểm y tế.
- **Không chẩn đoán y khoa (Medical Diagnosis)**, không đưa ra phác đồ điều trị, không kê đơn thuốc và không tự động điều động xe cấp cứu/cứu nạn y tế khẩn cấp. Mọi thông điệp trên hệ thống đều là sàng lọc sơ bộ và khuyến khích tìm đến trợ giúp y tế chuyên nghiệp.

### 1.3 Định nghĩa, từ viết tắt và thuật ngữ

| Thuật ngữ / Từ viết tắt | Định nghĩa đầy đủ |
|---|---|
| **API Gateway** | Điểm truy cập duy nhất (Port 8079) điều hướng toàn bộ request từ Client tới các Microservices nội bộ, thực thi xác thực JWT và bảo vệ giả mạo Header. |
| **Auth Service** | Microservice (Port 8081) quản lý tài khoản, phân quyền, xác thực email, quản lý thông báo, bookmark và cài đặt người dùng (`auth_schema`). |
| **Emotion Service** | Microservice (Port 8083) quản lý nhật ký cảm xúc, đo lường sức khỏe, bài đánh giá tâm lý, phân tích nguy cơ và kế hoạch tự chăm sóc (`emotion_schema`). |
| **AI Service** | Microservice (Port 8084) quản lý cơ sở tri thức, thực thi RAG (Retrieval-Augmented Generation), hội thoại AI và kiểm soát an toàn khủng hoảng (`ai_schema`). |
| **RAG** | Retrieval-Augmented Generation: Kỹ thuật trích xuất thông tin liên quan từ tài liệu đã kiểm duyệt trước khi đưa vào mô hình ngôn ngữ lớn để sinh câu trả lời có trích dẫn. |
| **RRF** | Reciprocal Rank Fusion: Thuật toán hợp nhất thứ hạng tìm kiếm ngữ nghĩa (Semantic Vector Search) và tìm kiếm từ khóa (Full-Text Search) với hằng số chuẩn $k=60$. |
| **PHQ-9** | Patient Health Questionnaire-9: Thang đo sàng lọc mức độ trầm cảm gồm 9 câu hỏi chuẩn hóa (0 - 27 điểm). |
| **GAD-7** | Generalized Anxiety Disorder-7: Thang đo sàng lọc mức độ rối loạn lo âu lan tỏa gồm 7 câu hỏi chuẩn hóa (0 - 21 điểm). |
| **WHO-5** | World Health Organization-Five Well-Being Index: Chỉ số đánh giá cảm nhận hạnh phúc/sức khỏe tâm lý (0 - 100 điểm chuẩn hóa). |
| **PSS-10** | Perceived Stress Scale: Thang đo cảm nhận căng thẳng gồm 10 câu hỏi (sử dụng theo dõi diễn tiến, không gán nhãn lâm sàng). |
| **Health Connect** | Nền tảng hợp nhất dữ liệu sức khỏe trên hệ điều hành Android do Google phát triển. |
| **Idempotency-Key** | Khóa định danh duy nhất trong header HTTP ngăn chặn việc xử lý trùng lặp giao dịch khi có retry mạng. |
| **RBAC** | Role-Based Access Control: Kiểm soát truy cập dựa trên vai trò người dùng (`ROLE_USER`, `ROLE_ADMIN`). |

### 1.4 Tài liệu tham khảo
1. `BUSINESS_SCOPE.md` - Giới hạn phạm vi nghiệp vụ và tuyên bố từ chối trách nhiệm y tế của MindCare.
2. `PRIVACY_DATA_RIGHTS.md` - Quy trình thực thi quyền riêng tư, kết xuất và hủy dữ liệu người dùng.
3. `docs/00-system-context.md` đến `docs/16-assessment-benchmarks-and-evidence.md` trong thư mục `emotion-service/docs/`.
4. `ai-service/docs/RAG-ARCHITECTURE.md`, `CRISIS-SAFETY-WORKFLOW.md`, `CONVERSATION-HISTORY.md`.
5. Kroenke, K., Spitzer, R. L., & Williams, J. B. (2001). The PHQ-9: validity of a brief depression severity measure. *Journal of General Internal Medicine*, 16(9), 606-613.
6. Spitzer, R. L., Kroenke, K., Williams, J. B., & Löwe, B. (2006). A brief measure for assessing generalized anxiety disorder: the GAD-7. *Archives of Internal Medicine*, 166(10), 1092-1097.

### 1.5 Tổng quan tài liệu
Tài liệu được cấu trúc thành 7 phần chính theo tiêu chuẩn kỹ thuật phần mềm. Các phần tiếp theo trình bày chi tiết về kiến trúc tổng thể, mô tả tác nhân, đặc tả chi tiết từng API/Use Case chức năng, các ràng buộc phi chức năng nghiêm ngặt và sơ đồ thiết kế cơ sở dữ liệu.

---

## 2. MÔ TẢ TỔNG QUAN HỆ THỐNG (OVERALL DESCRIPTION)

### 2.1 Bối cảnh sản phẩm & Sơ đồ kiến trúc logic

MindCare được thiết kế theo kiến trúc Microservices Monorepo hiện đại. Người dùng tương tác thông qua hai giao diện Client: **Web Application (React + Vite)** và **Mobile Application (Expo SDK 54 / React Native)**. 

Toàn bộ lưu lượng truyền thông Client-Server bắt buộc phải đi qua **API Gateway (Spring Cloud Gateway)**. API Gateway đóng vai trò tường lửa lớp ứng dụng (Reverse Proxy), xử lý CORS, xác thực Token JWT tập trung thông qua Auth Service, làm sạch (sanitize) các HTTP Header do client tự gửi và chuyển tiếp định danh đã xác thực (`X-User-Id`, `X-User-Role`) vào các Microservices nội bộ.

```
+-----------------------------------------------------------------------------------+
|                                  CLIENT LAYER                                     |
|    +----------------------------------+    +---------------------------------+    |
|    |      MindCare Web Application    |    |     MindCare Mobile Application |    |
|    |      (React 19 + TypeScript)     |    |    (React Native + Expo SDK 54) |    |
|    |         Port: 5173 (Vite)        |    |       Port: 8086 (Metro Dev)    |    |
|    +-----------------+----------------+    +----------------+----------------+    |
+----------------------|--------------------------------------|---------------------+
                       | HTTP REST / WebSocket                | HTTP REST / HealthConnect
                       v                                      v
+-----------------------------------------------------------------------------------+
|                                API GATEWAY LAYER                                  |
|     +-----------------------------------------------------------------------+     |
|     |           Spring Cloud Gateway (WebFlux) - Port: 8079                 |     |
|     |  - Route Matching & CORS Configuration                                |     |
|     |  - VerifiedIdentityGlobalFilter: Header Sanitization (Strip X-User-*) |     |
|     |  - Centralized Auth Verification via /api/auth/me                     |     |
|     |  - Downstream Injection: X-User-Id, X-User-Role                       |     |
|     +-----------------------------------+-----------------------------------+     |
+-----------------------------------------|-----------------------------------------+
                                          |
        +---------------------------------+---------------------------------+
        |                                 |                                 |
        v                                 v                                 v
+-----------------------+     +-----------------------+     +-----------------------+
|     AUTH SERVICE      |     |    EMOTION SERVICE    |     |      AI SERVICE       |
|   (Spring Boot 4.1)   |     |   (Spring Boot 4.1)   |     |   (Spring Boot 4.1)   |
|      Port: 8081       |     |      Port: 8083       |     |      Port: 8084       |
|-----------------------|     |-----------------------|     |-----------------------|
| - Authentication      |     | - Emotion Journaling  |     | - RAG Pipeline        |
| - Email OTP (6 số)    |     | - Emotion Trends      |     | - pgvector Search     |
| - Role Authorization  |     | - Standard Assessment |     | - Gemini Embedding-2  |
| - User Data Rights    |     | - Health Signals Sync |     | - Gemini 3.1 Flash    |
| - Notification Engine |     | - Risk Alert Engine   |     | - Crisis Safety Gate  |
| - Push Device Registry|     | - Self-Care Plans     |     | - Chat History        |
| - Bookmarks System    |     | - Source Consent Mgmt |     | - WebSocket Chat      |
+-----------+-----------+     +-----------+-----------+     +-----------+-----------+
            |                             |                             |
            +-----------------------------+-----------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                            DATA & INFRASTRUCTURE LAYER                            |
|  +-----------------------------------------------------------------------------+  |
|  |             PostgreSQL 16 Engine with pgvector (Port: 5432/55432)           |  |
|  |  - auth_schema: users, roles, email_verifications, notifications, bookmarks |  |
|  |  - emotion_schema: emotion_journals, assessments, results, health_metrics   |  |
|  |  - ai_schema: knowledge_documents, chunks, ai_conversations, messages     |  |
|  +-----------------------------------------------------------------------------+  |
|  +-------------------------------------+ +-------------------------------------+  |
|  | Mailpit / SMTP Server (1025 / 8025) | | Google Gemini API (Cloud Services)  |  |
|  +-------------------------------------+ +-------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

### 2.2 Các phân hệ và chức năng tổng thể

Hệ thống được chia thành 9 phân hệ chức năng cốt lõi:
1. **Phân hệ Quản lý Xác thực & Hồ sơ (Authentication & Identity)**: Đăng ký với cơ chế gửi mã xác thực 6 số qua email, đăng nhập cấp phát token JWT, cập nhật hồ sơ, tải lên ảnh đại diện, đổi mật khẩu.
2. **Phân hệ Quyền riêng tư dữ liệu (Privacy & Data Rights)**: Hỗ trợ quyền của chủ thể dữ liệu (Data Subject Rights): trích xuất toàn bộ dữ liệu máy đọc được (`/data-export`) và quy trình xóa tài khoản vĩnh viễn không thể phục hồi theo thứ tự cascade chặt chẽ.
3. **Phân hệ Nhật ký Cảm xúc & Xu hướng (Emotion Journal & Analytics)**: Ghi lại trạng thái cảm xúc, các chỉ số năng lượng/căng thẳng/giấc ngủ; tính toán biểu đồ xu hướng cảm xúc theo Ngày/Tuần/Tháng theo chuẩn múi giờ IANA.
4. **Phân hệ Đánh giá Tâm lý Chuẩn hóa (Psychological Assessments)**: Danh mục các bài kiểm tra trắc nghiệm tâm lý (PHQ-9, GAD-7, WHO-5, PSS-10), chấm điểm tự động từ server, phân loại mức độ rủi ro kèm khuyến nghị an toàn.
5. **Phân hệ Đồng bộ Tín hiệu Sức khỏe (Health Signals Ingestion)**: Tiếp nhận dữ liệu bước chân, nhịp tim nghỉ, chu kỳ giấc ngủ từ Google Health Connect/Apple HealthKit, kiểm soát trạng thái cấp phép và quyền hủy dữ liệu nguồn sức khỏe.
6. **Phân hệ Trợ lý AI & RAG (AI Assistant & Knowledge Retrieval)**: Trò chuyện tương tác với AI được tiếp đất (grounded) bằng nguồn tài liệu chuyên môn đã qua thẩm duyệt, phát hiện dấu hiệu khủng hoảng tự hại bằng bộ lọc từ khóa chuyên biệt (Crisis Safety Gate).
7. **Phân hệ Kế hoạch Tự chăm sóc & Bookmarks (Self-Care & Bookmarks)**: Thiết lập kế hoạch mục tiêu vi mô tuần, điểm danh hoạt động tự chăm sóc hàng ngày, quản lý bookmark nội dung và bài đánh giá.
8. **Phân hệ Nhắc nhở & Thông báo (Reminders & Notifications)**: Cài đặt lịch nhắc check-in cảm xúc, lưu trữ token thiết bị di động (Android/iOS) và đẩy thông báo real-time qua WebSocket.
9. **Phân hệ Quản trị Hệ thống (Admin Management)**: Quản lý người dùng, quản trị phiên bản bộ câu hỏi đánh giá (Draft -> Published -> Archived), quản trị tài liệu tri thức RAG và kích hoạt re-index vector.

### 2.3 Đối tượng người dùng & Phân quyền (User Classes & Actors)

Hệ thống định nghĩa 2 nhóm tác nhân con người và các hệ thống bên ngoài:

| Nhóm tác nhân (Actor) | Vai trò hệ thống (Role) | Mô tả đặc điểm & Quyền hạn |
|---|---|---|
| **Người dùng cuối (End User)** | `ROLE_USER` | Học sinh, sinh viên, người trẻ tuổi có nhu cầu chăm sóc sức khỏe tinh thần. Được toàn quyền tạo/xem nhật ký của mình, làm bài trắc nghiệm tâm lý, đồng bộ dữ liệu sức khỏe, tương tác với AI Assistant, lập kế hoạch tự chăm sóc, xuất dữ liệu và yêu cầu xóa tài khoản. |
| **Quản trị viên (Admin)** | `ROLE_ADMIN` | Đội ngũ quản trị hệ thống. Được quyền xem danh sách người dùng, kích hoạt/khóa tài khoản, tạo và biên tập phiên bản bài trắc nghiệm (Draft), xuất bản (Publish) hoặc lưu trữ (Archive) bài test, đăng tải tài liệu tri thức RAG, phê duyệt nguồn tin và tái lập chỉ mục (Reindex). Quản trị viên **không** được quyền đọc nhật ký riêng tư của người dùng. |
| **Hệ thống thiết bị (Device/OS)** | *External System* | Google Health Connect / Apple HealthKit cung cấp quyền đọc cảm biến sức khỏe từ thiết bị di động của người dùng. |
| **Dịch vụ AI bên ngoài** | *External Cloud* | Google Gemini API (Generative AI & Text Embeddings) xử lý vector hóa và sinh văn bản hội thoại có kiểm soát. |
| **Hệ thống gửi Email** | *External Service* | Dịch vụ Mail Server SMTP (Google Gmail SMTP trong môi trường Production hoặc Mailpit trong môi trường Local) phục vụ chuyển phát mã OTP xác minh tài khoản. |

### 2.4 Môi trường vận hành (Operating Environment)

- **Hệ điều hành Server**: Windows Server / Linux (Ubuntu 22.04 LTS / Debian 12 / Docker Container).
- **Runtime Backend**: Java OpenJDK 17 LTS, Spring Boot Framework 4.1.0-SNAPSHOT (hoặc tương đương bản Spring Boot 3.x), Spring Cloud 2025.1.2.
- **Hệ cơ sở dữ liệu**: PostgreSQL 16.8 tích hợp extension `vector` (pgvector v0.5.1).
- **Môi trường Web Client**: Trình duyệt hiện đại hỗ trợ ECMAScript 2022+ (Google Chrome 110+, Mozilla Firefox 110+, Safari 16+, Microsoft Edge 110+).
- **Môi trường Mobile Client**: Thiết bị Android phiên bản 10.0+ (hỗ trợ Google Health Connect), iOS phiên bản 15.0+ (hỗ trợ Apple HealthKit).

### 2.5 Ràng buộc thiết kế và triển khai

1. **Ràng buộc cô lập dữ liệu (Database Schema Separation)**: Hệ thống sử dụng chung một cơ sở dữ liệu PostgreSQL vật lý nhưng chia thành 3 schema riêng biệt: `auth_schema`, `emotion_schema`, `ai_schema`. Tuyệt đối không tạo Foreign Key liên-schema (Cross-schema Foreign Keys). Mối quan hệ giữa các thực thể chỉ được tham chiếu thông qua giá trị UUID nguyên bản.
2. **Ràng buộc Gateway Header Sanitization**: Mọi truy cập vào Microservices bên trong phải đi qua Gateway. Gateway có trách nhiệm xóa sạch các header `X-User-Id` và `X-User-Role` từ client gửi lên để ngăn ngừa triệt để lỗi giả mạo định danh (Header Injection / Identity Spoofing).
3. **Ràng buộc Idempotency**: Các thao tác gửi dữ liệu nhạy cảm hoặc batch data (như nộp bài trắc nghiệm tâm lý, đồng bộ batch dữ liệu sức khỏe thiết bị) bắt buộc phải kèm theo Header `Idempotency-Key` (chuỗi ký tự UUID hoặc băm ngẫu nhiên tối đa 255 ký tự).
4. **Ràng buộc tính bất biến (Immutability)**:
   - Bài đánh giá sau khi đã chuyển sang trạng thái `PUBLISHED` là bất biến. Không được phép chỉnh sửa câu hỏi hay thang điểm trên bản đã phát hành. Muốn thay đổi nội dung bắt buộc phải kích hoạt quy trình tạo phiên bản mới (`POST :create-next-version`).
   - Nhật ký cảm xúc không hỗ trợ API chỉnh sửa (No update use case). Chỉ hỗ trợ thao tác xóa mềm (soft-delete) trong cửa sổ 15 phút đầu tiên kể từ thời điểm tạo.
5. **Ràng buộc chuẩn hóa thời gian**: Toàn bộ mốc thời gian lưu trữ trong cơ sở dữ liệu phải là kiểu `TIMESTAMPTZ` (UTC). Các API nhận và trả về thời gian theo định dạng ISO-8601 có chỉ số offset múi giờ (ví dụ: `2026-08-27T08:30:00+07:00`).

### 2.6 Giả định và sự phụ thuộc

- Người dùng sở hữu một hộp thư điện tử (email) hợp lệ có thể nhận mã xác thực OTP 6 số để kích hoạt tài khoản.
- Điện thoại của người dùng đã cài đặt và kích hoạt dịch vụ Google Health Connect (đối với hệ điều hành Android) hoặc Apple Health (iOS) và cấp quyền truy cập các chỉ số tương ứng cho ứng dụng MindCare Mobile.
- Kết nối tới Google Gemini API được duy trì ổn định với độ trễ mạng chấp nhận được và API Key có định mức quota hợp lệ.

---

## 3. YÊU CẦU GIAO DIỆN HỆ THỐNG (EXTERNAL INTERFACE REQUIREMENTS)

### 3.1 Giao diện người dùng (User Interfaces)

- **Ứng dụng Web (MindCare Web App)**: 
  - Xây dựng bằng React 19, Vite, TypeScript, cấu trúc theo Feature Folders.
  - Hỗ trợ thiết kế responsive (Desktop, Tablet).
  - Tích hợp các trang: Đăng nhập/Đăng ký xác thực OTP, Bảng tổng quan (Dashboard), Viết nhật ký cảm xúc & Xem biểu đồ xu hướng, Danh sách bài trắc nghiệm tâm lý & Giao diện làm bài, Cửa sổ Trò chuyện Trợ lý AI (phản hồi hoàn chỉnh qua REST/WebSocket), Kế hoạch tự chăm sóc tuần, Trang thiết lập quyền riêng tư & Xuất/Xóa dữ liệu, Trang Quản trị viên (Admin Portal).
- **Ứng dụng Di động (MindCare Mobile App)**:
  - Xây dựng bằng Expo SDK 54 / React Native, TypeScript.
  - Cấu trúc thanh điều hướng Tab phía dưới (Bottom Tabs Navigation):
    1. **Nhật ký (Journal)**: Check-in cảm xúc nhanh, ghi chú tâm trạng, thanh trượt mức năng lượng/stress.
    2. **Đánh giá (Assessments)**: Danh sách bài test tâm lý PHQ-9, GAD-7, WHO-5.
    3. **Trợ lý AI (AI Chat)**: Giao diện chat tương tác với phễu hỗ trợ an toàn khẩn cấp.
    4. **Sức khỏe (Health Connect)**: Trực quan hóa dữ liệu giấc ngủ, số bước, nhịp tim đồng bộ từ thiết bị.
    5. **Cài đặt (Settings)**: Quản lý hồ sơ, nhắc nhở, tùy chọn quyền riêng tư & xóa tài khoản.

### 3.2 Giao diện phần cứng (Hardware Interfaces)

- Thiết bị di động tích hợp cảm biến đếm bước chân (Pedometer), cảm biến đo nhịp tim (PPG sensor), và thiết bị đeo thông minh (Smartwatch/Fitness Tracker) kết nối thông qua Google Health Connect API hoặc Apple HealthKit API.

### 3.3 Giao diện phần mềm (Software Interfaces)

- **PostgreSQL 16 & pgvector Extension**: Hệ cơ sở dữ liệu quan hệ lưu trữ trạng thái giao dịch, đồng thời là cơ sở dữ liệu vector thực hiện tìm kiếm tương đồng vector Cosine HNSW (độ dài 768 chiều).
- **Google Gemini Generative AI API**:
  - Embedding Model: `gemini-embedding-2` (Output: 768 float dimensions).
  - Chat Model: `gemini-3.1-flash-lite` (hỗ trợ model fallback cấu hình qua biến môi trường).
- **Mail Server (SMTP Protocol)**: 
  - Môi trường Local: Mailpit Container (SMTP port 1025, Web UI port 8025).
  - Môi trường Production: Gmail SMTP (`smtp.gmail.com:587`, STARTTLS bắt buộc).
- **Push Notification Engines**: Giao diện đăng ký Push Token (FCM / APNs) trên thiết bị di động.

### 3.4 Giao diện truyền thông và giao thức (Communication Interfaces)

- **Giao thức mạng**: HTTPS (TLS 1.3) bắt buộc đối với toàn bộ lưu lượng public; WSS (Secure WebSocket) cho các kết nối truyền dữ liệu thời gian thực.
- **RESTful API**: 
  - Định dạng trao đổi dữ liệu: `application/json; charset=UTF-8`.
  - Quy chuẩn phản hồi lỗi đồng nhất: Bắt buộc trả về cấu trúc lỗi có mã máy đọc được (`code`), thông điệp ngắn (`message`), thời điểm phát sinh (`timestamp`) và mã định danh vết (`traceId` hoặc `X-Correlation-Id`).
- **Giao thức WebSocket**:
  - `/ws/ai`: Kênh WebSocket hai chiều giữa Client và AI Service. Hiện trả một `AI_RESPONSE` hoàn chỉnh sau kiểm tra citation, kèm `sources` và `safety`; chưa truyền từng token. REST và WebSocket dùng chung UUID `requestId` để chống xử lý trùng khi thử lại.
  - `/ws/notifications`: Kênh đẩy thông báo tức thời từ Auth Service tới Client khi có sự kiện hệ thống hoặc nhắc nhở.

---

## 4. YÊU CẦU CHỨC NĂNG CHI TIẾT (FUNCTIONAL REQUIREMENTS)

### 4.1 Phân hệ Xác thực & Hồ sơ cá nhân (Authentication & Profile)

#### FR-AUTH-01: Đăng ký tài khoản với xác thực Email OTP
- **Actor**: Khách vãng lai (Guest).
- **Input**: `email`, `password` (8 - 72 ký tự, bắt buộc chứa cả chữ cái và số), `fullName`.
- **Xử lý**: 
  1. Kiểm tra tính duy nhất của email trong `auth_schema.users`. Nếu đã tồn tại tài khoản hoạt động, trả về lỗi `EMAIL_ALREADY_EXISTS`.
  2. Tạo bản ghi tài khoản ở trạng thái `is_active = FALSE`.
  3. Sinh ngẫu nhiên mã số xác thực OTP gồm 6 chữ số (`000000` - `999999`), thời hạn hiệu lực chính xác 30 phút.
  4. Băm mã OTP bằng thuật toán SHA-256 trước khi lưu vào `auth_schema.email_verifications`. Tuyệt đối không lưu OTP dạng plain text.
  5. Gửi thư chứa mã OTP đến email người dùng trong cùng transaction đăng ký. Nếu SMTP từ chối gửi ngay lập tức, transaction được rollback toàn bộ.
- **Output**: HTTP 201 Created kèm thông điệp yêu cầu nhập mã xác minh gửi về hộp thư.

#### FR-AUTH-02: Xác thực Email kích hoạt tài khoản
- **Actor**: Khách vãng lai đã hoàn thành bước đăng ký.
- **Input**: `email`, `verificationCode` (chuỗi 6 chữ số).
- **Xử lý**: 
  1. Tìm kiếm bản ghi xác minh mới nhất tương ứng với email trong `auth_schema.email_verifications`.
  2. Kiểm tra điều kiện: Mã chưa được sử dụng (`used_at IS NULL`), chưa quá hạn (`expires_at > CURRENT_TIMESTAMP`), và giá trị băm SHA-256 của mã gửi lên khớp với cơ sở dữ liệu.
  3. Cập nhật `used_at = CURRENT_TIMESTAMP` và kích hoạt tài khoản `is_active = TRUE`.
- **Output**: HTTP 200 OK. Tài khoản sẵn sàng để đăng nhập.

#### FR-AUTH-03: Đăng nhập & Cấp phát JWT
- **Actor**: Người dùng đã kích hoạt tài khoản.
- **Input**: `email`, `password`.
- **Xử lý**: 
  1. Kiểm tra tài khoản có tồn tại và đang hoạt động (`is_active = TRUE`).
  2. Xác thực mật khẩu bằng thuật toán BCrypt. Nếu sai thông tin, trả về mã lỗi `INVALID_CREDENTIALS` (không phân biệt sai email hay sai mật khẩu để chống kỹ thuật thu thập tài khoản - Account Enumeration).
  3. Sinh chuỗi JWT chứa các claims tiêu chuẩn: `sub` (User ID dạng UUID), `role` (`ROLE_USER` hoặc `ROLE_ADMIN`), thời gian phát hành (`iat`), thời gian hết hạn (`exp`).
- **Output**: HTTP 200 OK kèm JSON chứa `accessToken`, `tokenType: Bearer`, thông tin tóm tắt người dùng (`id`, `email`, `fullName`, `role`).

#### FR-AUTH-04: Cập nhật hồ sơ cá nhân & Tải lên Avatar
- **Actor**: Người dùng đã xác thực (`ROLE_USER`, `ROLE_ADMIN`).
- **Input**: `fullName`, `phoneNumber`, `bio`, file ảnh nhị phân (`multipart/form-data`).
- **Xử lý**: 
  1. Validate định dạng ảnh (chỉ chấp nhận JPEG, PNG, WEBP), kích thước tối đa 5MB.
  2. Lưu trữ file avatar vào thư mục lưu trữ cục bộ bảo vệ hoặc Object Storage, cập nhật đường dẫn `avatar_url` trong `auth_schema.users`.
  3. Cập nhật các trường thông tin cá nhân.
- **Output**: HTTP 200 OK kèm hồ sơ cập nhật.

---

### 4.2 Phân hệ Quyền riêng tư & Quản lý Dữ liệu cá nhân (Privacy & Data Rights)

#### FR-PRIV-01: Xuất dữ liệu cá nhân tổng hợp (Data Export)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Xử lý**: 
  1. Tổng hợp toàn bộ dữ liệu thuộc quyền sở hữu của `userId` từ cả 3 microservices:
     - Từ `auth-service`: Hồ sơ người dùng, các thông báo hệ thống, danh sách bookmark, tùy chọn nhắc nhở.
     - Từ `emotion-service`: Lịch sử nhật ký cảm xúc, các bài đánh giá tâm lý đã làm, dữ liệu sức khỏe đã đồng bộ, kế hoạch tự chăm sóc.
     - Từ `ai-service`: Danh sách các cuộc trò chuyện và lịch sử tin nhắn hỏi đáp với AI.
  2. Loại bỏ toàn bộ thông tin bảo mật nội bộ: Chuỗi băm mật khẩu (`password_hash`), token khôi phục, mã OTP, prompt ẩn của hệ thống, trọng số phân loại nguy cơ nội bộ.
  3. Định dạng toàn bộ dữ liệu thành cấu trúc JSON chuẩn hóa (Machine-Readable Format).
- **Output**: HTTP 200 OK với file/payload JSON chứa toàn bộ dữ liệu cuộc đời số của người dùng tại MindCare.

#### FR-PRIV-02: Quy trình Xóa sổ Tài khoản & Dữ liệu Vĩnh viễn (Permanent Erasure Cascade)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: `password` (Mật khẩu xác nhận hành động nguy hiểm).
- **Xử lý**: Tuân thủ nghiêm ngặt quy trình 5 bước theo thứ tự định sẵn tại `PRIVACY_DATA_RIGHTS.md`:
  1. **Bước 1**: Xác thực lại mật khẩu hiện tại thông qua endpoint `POST /api/auth/me/verify-password`. Nếu không khớp, lập tức dừng quy trình và trả về lỗi 401.
  2. **Bước 2**: Gọi endpoint `DELETE /api/v1/privacy/data` tại `emotion-service` để xóa toàn bộ nhật ký cảm xúc, kết quả trắc nghiệm tâm lý, dữ liệu sức khỏe và kế hoạch tự chăm sóc của người dùng (xử lý Idempotent).
  3. **Bước 3**: Gọi endpoint `DELETE /api/ai/privacy/data` tại `ai-service` để xóa vĩnh viễn toàn bộ các cuộc hội thoại và tin nhắn AI của người dùng (xử lý Idempotent).
  4. **Bước 4**: Thực hiện xóa vĩnh viễn tài khoản người dùng tại `auth-service` (`DELETE /api/auth/me/permanent`), kích hoạt xóa liên đới (Cascade Delete) toàn bộ các bản ghi phiên làm việc, mã xác thực, thông báo, thiết bị push và bookmark trong cơ sở dữ liệu.
  5. **Bước 5**: Xóa vĩnh viễn file ảnh đại diện (avatar) lưu cục bộ trên ổ đĩa máy chủ sau khi transaction cơ sở dữ liệu commit thành công.
- **Đặc tính kỹ thuật**: Microservices Emotion và AI xử lý xóa có tính chất Idempotent. Tài khoản tại Auth Service được xóa ở bước cuối cùng để người dùng vẫn duy trì quyền ủy quyền gửi lại yêu cầu (Retry) nếu xảy ra sự cố gián đoạn mạng giữa chừng.

---

### 4.3 Phân hệ Nhật ký Cảm xúc & Phân tích Xu hướng (Emotion Journal & Analytics)

#### FR-EMO-01: Tạo bản ghi Nhật ký Cảm xúc (Check-in)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: 
  - `emotionType` (Bắt buộc, thuộc danh mục: `VERY_HAPPY`, `HAPPY`, `NEUTRAL`, `SAD`, `STRESSED`).
  - `content` (Tùy chọn, tối đa 5.000 ký tự Unicode).
  - `energyLevel` (Tùy chọn, số nguyên từ 1 đến 5: 1 rất thấp, 5 rất cao).
  - `stressLevel` (Tùy chọn, số nguyên từ 1 đến 5: 1 rất ít, 5 rất nhiều).
  - `sleepQuality` (Tùy chọn, số nguyên từ 1 đến 5: 1 rất kém, 5 rất tốt).
- **Xử lý**: 
  1. Định danh `userId` được trích xuất trực tiếp từ Security Context (header `X-User-Id` đã kiểm duyệt), cấm nhận từ body request.
  2. Chuẩn hóa chuỗi văn bản `content` (cắt tỉa khoảng trắng, chuẩn hóa ký tự xuống dòng).
  3. Gán thời điểm tạo `createdAt = CURRENT_TIMESTAMP` (UTC).
  4. Không hỗ trợ bất kỳ API nào cho phép chỉnh sửa nội dung nhật ký sau khi đã ghi nhận (Bảo toàn tính trung thực của trạng thái tâm lý).
- **Output**: HTTP 201 Created kèm đối tượng nhật ký hoàn chỉnh và ID định danh UUID.

#### FR-EMO-02: Xem Lịch sử Nhật ký Cảm xúc có phân trang
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: Tham số truy vấn `from`, `to` (khoảng thời gian lọc `[from, to)`, tối đa 365 ngày), `cursor` (keyset pagination), `limit` (1 - 100, mặc định 20).
- **Xử lý**: 
  1. Chỉ truy vấn các bản ghi có `user_id = :currentUserId` và `deleted_at IS NULL`.
  2. Sắp xếp theo cặp khóa `(created_at, id)` giảm dần.
- **Output**: HTTP 200 OK kèm danh sách bản ghi và con trỏ `nextCursor`.

#### FR-EMO-03: Xóa mềm Nhật ký Cảm xúc (Soft Delete với Time Window)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: `journalId` (UUID của bản ghi nhật ký cần xóa).
- **Xử lý**: 
  1. Kiểm tra quyền sở hữu bản ghi (`user_id = :currentUserId`) và chưa bị xóa trước đó. Nếu không khớp hoặc đã xóa, trả về mã lỗi 404 Không tìm thấy.
  2. **Kiểm tra cửa sổ xóa hợp lệ**: Người dùng chỉ được phép xóa bản ghi trong vòng **15 phút** kể từ thời điểm tạo (`CURRENT_TIMESTAMP <= createdAt + 15 phút`).
  3. Nếu quá 15 phút, từ chối xóa và trả về mã lỗi nghiệp vụ `JOURNAL_DELETION_WINDOW_EXPIRED`.
  4. Nếu trong khoảng 15 phút, cập nhật `deleted_at = CURRENT_TIMESTAMP`.
- **Output**: HTTP 204 No Content.

#### FR-EMO-04: Phân tích & Trực quan hóa Xu hướng Cảm xúc (Emotion Trends)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: Tham số truy vấn `from`, `to` (khoảng thời gian, tối đa 730 ngày), `bucket` (`DAY`, `WEEK`, `MONTH`), `timezone` (Tên múi giờ IANA hợp lệ, ví dụ: `Asia/Ho_Chi_Minh`).
- **Xử lý**: 
  1. Áp dụng bảng quy đổi điểm số chuẩn hóa phiên bản `emotion-v1`:
     - `VERY_HAPPY` = +2 điểm
     - `HAPPY` = +1 điểm
     - `NEUTRAL` = 0 điểm
     - `SAD` = -1 điểm
     - `STRESSED` = -2 điểm
  2. Gom nhóm các bản ghi nhật ký hợp lệ (loại trừ các bản ghi có `deleted_at IS NULL`) vào các chu kỳ thời gian (Bucket) liên tục trên khoảng `[from, to)`. Tuần được tính bắt đầu từ thứ Hai; Tháng tính từ ngày 01.
  3. Đối với mỗi bucket, tính toán: `count` (số lượt check-in), `averageScore = tổng điểm / count` (làm tròn 2 chữ số thập phân theo quy tắc `HALF_UP`).
  4. Nếu một bucket không có bất kỳ lượt check-in nào, trả về `count = 0` và `averageScore = null` (Không được tự ý giả định là điểm 0 / Neutral).
- **Output**: HTTP 200 OK kèm chuỗi dữ liệu xu hướng liên tục phục vụ vẽ biểu đồ.

---

### 4.4 Phân hệ Đánh giá Tâm lý Chuẩn hóa (Psychological Assessments)

#### FR-ASM-01: Xem Danh mục Bài Đánh giá đang phát hành
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Xử lý**: Truy vấn danh mục các bài đánh giá có trạng thái `status = 'PUBLISHED'` và `deleted_at IS NULL`. Mỗi mã trắc nghiệm (Code) tối đa chỉ có 1 phiên bản phát hành duy nhất.
- **Output**: HTTP 200 OK trả về mảng thông tin tóm tắt gồm: `id`, `code` (ví dụ: `PHQ-9`, `GAD-7`, `WHO-5`, `PSS-10`), `assessmentVersion`, `title`, `description`. Không trả về danh sách câu hỏi, thang điểm hay trạng thái vòng đời.

#### FR-ASM-02: Lấy Nội dung Chi tiết Bài Đánh giá để làm bài
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: `code` trên đường dẫn URL (ví dụ: `/api/v1/assessments/PHQ-9`).
- **Xử lý**: 
  1. Kiểm tra tính hợp lệ của mã trắc nghiệm và tìm phiên bản `PUBLISHED` đang hoạt động.
  2. Lấy toàn bộ danh sách câu hỏi (`questions`) và danh sách phương án trả lời (`answer_options`) được sắp xếp tăng dần theo `orderIndex`.
  3. **Bảo mật thang đo**: Phương án trả lời trả về cho Client chỉ chứa `id` và `optionText`. Tuyệt đối không để lộ trường điểm số `scoreValue` về phía giao diện người dùng.
- **Output**: HTTP 200 OK kèm cấu trúc bài test chuẩn bị làm.

#### FR-ASM-03: Nộp bài trắc nghiệm & Tính điểm Tự động (Submission & Scoring)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Header**: Bắt buộc có `Idempotency-Key` (chuỗi ký tự không rỗng, tối đa 255 ký tự).
- **Input**: `assessmentVersion`, danh sách câu trả lời `answers: [{ questionId, selectedOptionId }]`.
- **Xử lý**: 
  1. Kiểm tra `Idempotency-Key`: Nếu khóa đã được xử lý trước đó với cùng payload, trả về ngay kết quả đã lưu trong cơ sở dữ liệu mà không tính toán lại. Nếu cùng khóa nhưng payload khác nhau, trả về lỗi `IDEMPOTENCY_CONFLICT`.
  2. Xác thực phiên bản nộp bài phải khớp chính xác với phiên bản `PUBLISHED` hiện hành của mã trắc nghiệm đó.
  3. Xác thực số lượng câu trả lời phải đầy đủ 100% số câu hỏi, mỗi câu hỏi chọn duy nhất 1 phương án và phương án phải thuộc về câu hỏi đó.
  4. **Tính điểm tập trung tại Server**: Lấy điểm số `scoreValue` từ cơ sở dữ liệu cho từng phương án, áp dụng chính sách chấm điểm xác định (Deterministic Scoring Policy):
     - **PHQ-9**: Tổng điểm từ 9 câu (mỗi câu 0-3 điểm, tổng từ 0-27). Phân loại: 0-4 (Tối thiểu/Bình thường), 5-9 (Nhẹ), 10-14 (Vừa), 15-19 (Nặng vừa), 20-27 (Nặng). Câu hỏi số 9 (ý nghĩ tự hại) được giám sát riêng làm tín hiệu an toàn độc lập.
     - **GAD-7**: Tổng điểm từ 7 câu (mỗi câu 0-3 điểm, tổng từ 0-21). Phân loại: 0-4 (Bình thường), 5-9 (Lo âu nhẹ), 10-14 (Lo âu vừa), 15-21 (Lo âu nặng).
     - **WHO-5**: Điểm thô từ 5 câu (0-25 điểm). Điểm chuẩn hóa = Điểm thô $\times 4$ (Thang 0-100). Điểm dưới 50 phản ánh mức độ hạnh phúc/sức khỏe tâm lý thấp.
     - **PSS-10**: Đảo ngược thang điểm các câu 4, 5, 7, 8 (từ 0-4 thành 4-0), tính tổng điểm. Phục vụ theo dõi diễn tiến căng thẳng (Tracking only), không gán nhãn chẩn đoán lâm sàng.
  5. Lưu trữ kết quả đánh giá vào `emotion_schema.assessment_results`, lưu vết toàn bộ phiên bản thuật toán, snapshot câu trả lời, thông điệp sàng lọc và khuyến nghị an toàn đã duyệt.
- **Output**: HTTP 201 Created kèm tổng điểm, phân loại nguy cơ và các chỉ dẫn tự chăm sóc an toàn (kèm tuyên bố từ chối trách nhiệm y tế).

#### FR-ASM-04: Xem Lịch sử & Chi tiết Kết quả Đánh giá
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Xử lý**: 
  - Xem danh sách: Trả về các kết quả đánh giá thuộc quyền sở hữu của người dùng hiện tại theo khoảng thời gian và phân trang keyset.
  - Xem chi tiết: Kiểm tra nghiêm ngặt `id`, `user_id` và `deleted_at IS NULL`. Nếu bản ghi không tồn tại hoặc thuộc về người dùng khác, đều trả về mã lỗi 404 để chống rò rỉ thông tin.
- **Output**: HTTP 200 OK kèm thông tin sàng lọc chi tiết.

---

### 4.5 Phân hệ Đồng bộ Dữ liệu Sức khỏe (Health Metrics Ingestion)

#### FR-HLT-01: Tiếp nhận Batch Dữ liệu Sức khỏe từ Thiết bị
- **Actor**: Thiết bị di động của người dùng (`ROLE_USER`).
- **Header**: Bắt buộc có `Idempotency-Key`.
- **Input**: `sourceType` (`HEALTH_CONNECT`, `APPLE_HEALTH`, `MANUAL`), danh sách các bản ghi sức khỏe (tối đa 100 bản ghi/lần gửi):
  - `metricType`: `SLEEP_HOURS`, `HEART_RATE`, `STEP_COUNT`, `SLEEP_SESSION`, `EXERCISE_SESSION`.
  - `metricValue`, `unit` (chuẩn hóa: `h` cho giờ ngủ, `bpm` cho nhịp tim, `count` cho số bước).
  - `recordedAt`, `startTime`, `endTime`, `externalSampleId` (mã định danh duy nhất của mẫu trên hệ điều hành thiết bị).
- **Xử lý**: 
  1. Kiểm tra trạng thái cấp phép nguồn sức khỏe trong `emotion_schema.health_source_consents`. Nếu người dùng đã thu hồi quyền của nguồn này, từ chối toàn bộ request với lỗi `HEALTH_SOURCE_CONSENT_REQUIRED`.
  2. Validate toàn bộ batch dữ liệu theo nguyên tắc Atomic: Kiểm tra thời gian không được ở tương lai quá 5 phút (clock skew), không được gửi lùi về quá khứ quá 30 ngày. Nếu có bất kỳ bản ghi nào không hợp lệ, hủy bỏ toàn bộ batch.
  3. Thực hiện Upsert (Chèn hoặc Cập nhật) dựa trên bộ ba `(user_id, source_type, external_sample_id)`. Nếu bản ghi đã tồn tại nhưng có thời điểm chỉnh sửa nguồn (`sourceLastModifiedAt`) mới hơn, cập nhật lại dữ liệu.
- **Output**: HTTP 200 OK kèm số lượng bản ghi đã xử lý thành công.

#### FR-HLT-02: Kiểm soát Cấp phép & Hủy Dữ liệu Nguồn Sức khỏe (Source Consent & Erasure)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **API**:
  - `GET /api/v1/health-metrics/sources/{sourceType}`: Kiểm tra số lượng bản ghi, khoảng thời gian đã lưu và trạng thái kích hoạt của nguồn.
  - `POST /api/v1/health-metrics/sources/{sourceType}/enable`: Cho phép tiếp tục đồng bộ dữ liệu từ nguồn.
  - `DELETE /api/v1/health-metrics/sources/{sourceType}/data`: Thu hồi vĩnh viễn quyền đồng bộ và thực hiện **xóa vật lý toàn bộ bản ghi sức khỏe** cùng metadata idempotency của nguồn đó trong cơ sở dữ liệu `emotion_schema`.
- **Output**: HTTP 200 OK / 204 No Content.

---

### 4.6 Phân hệ Trợ lý AI & Tra cứu Tri thức RAG (AI Assistant & RAG)

#### FR-AI-01: Quản trị Tài liệu Tri thức Chuyên môn (Knowledge Ingestion & Governance)
- **Actor**: Quản trị viên hệ thống (`ROLE_ADMIN`).
- **Input**: File tài liệu (PDF, TXT, Markdown) hoặc văn bản trực tiếp, tiêu đề, tác giả, nhà xuất bản, năm xuất bản, thứ hạng tin cậy nguồn (`sourceTier`: `A`, `B`, `C`, `D`), phạm vi bằng chứng và giới hạn.
- **Xử lý**: 
  1. Chỉ các tài liệu thuộc Tier A (sách giáo trình, cẩm nang y tế chính thống, hướng dẫn WHO) hoặc Tier B (nghiên cứu khoa học có bình duyệt) mới đủ điều kiện phê duyệt đưa vào kho tri thức RAG.
  2. Tài liệu mới tải lên mặc định ở trạng thái `review_status = 'NEEDS_REVIEW'` và `is_active = FALSE`.
  3. Sau khi Admin kiểm duyệt và kích hoạt, tài liệu được phân tách văn bản thành các đoạn (chunking) nhận biết câu hoàn chỉnh: Độ dài chuẩn 1.800 ký tự, độ gối đầu (overlap) 250 ký tự.
  4. Mỗi chunk được gửi tới mô hình `gemini-embedding-2` để sinh vector nhúng 768 chiều và lưu vào bảng `ai_schema.knowledge_chunks`. Đồng thời, cột `search_vector` (tsvector) tự động được cập nhật phục vụ tìm kiếm từ khóa.
- **Output**: HTTP 201 Created kèm số lượng chunk và trạng thái vector hóa.

#### FR-AI-02: Tìm kiếm Lai (Hybrid Search) & Hợp nhất Thứ hạng RRF
- **Actor**: Hệ thống AI Service (Thực thi ngầm trong luồng chat).
- **Quy trình xử lý**:
  1. Nhận câu hỏi của người dùng, sinh vector nhúng cho câu hỏi thông qua `gemini-embedding-2`.
  2. Thực hiện song song 2 truy vấn trên cơ sở dữ liệu PostgreSQL (`ai_schema.knowledge_chunks`):
     - **Semantic Vector Search**: Đo khoảng cách Cosine thông qua toán tử `<=>` trên chỉ mục HNSW vector. Lấy Top-K kết quả gần nhất.
     - **Full-Text Lexical Search**: Tìm kiếm từ khóa thông qua hàm `websearch_to_tsquery` trên chỉ mục GIN của cột `search_vector`. Lấy Top-K kết quả khớp từ khóa nhất.
  3. Áp dụng công thức Reciprocal Rank Fusion (RRF) với hằng số $k = 60$ để tính toán điểm số tổng hợp:
     $$RRF\_Score(d) = \sum_{m \in \{vector, text\}} \frac{1}{60 + Rank_m(d)}$$
  4. Lọc bỏ các nguồn đã hết hạn (`expires_at < CURRENT_TIMESTAMP`) hoặc chưa được phê duyệt (`APPROVED`). Chọn ra các chunk có điểm số RRF cao nhất để nạp vào Context đưa vào Prompt của mô hình ngôn ngữ lớn.

#### FR-AI-03: Phễu An toàn Khủng hoảng & Định tuyến Khẩn cấp (Crisis Safety Workflow)
- **Actor**: Người dùng tương tác với AI (`ROLE_USER`).
- **Cơ chế hoạt động**: 
  1. Phân loại mức độ rủi ro tự hại dựa trên thuật toán lọc từ khóa tiếng Việt tiền định (Deterministic Gate), **chỉ đánh giá nội dung tin nhắn hiện tại** của người dùng:
     - `NONE`: Không phát hiện dấu hiệu nguy hiểm. Luồng hội thoại diễn ra bình thường.
     - `CHECK_IN`: Phát hiện ngôn từ bế tắc, kiệt sức mơ hồ. Giao diện hiển thị một câu hỏi nhẹ nhàng thăm dò mức độ an toàn.
     - `EXPLICIT`: Phát hiện tuyên bố trực tiếp về ý định tự tử hoặc tự hại (ví dụ: "tôi muốn tự tử", "tôi muốn kết thúc cuộc đời").
     - `IMMINENT`: Phát hiện ý định tự hại đi kèm kế hoạch, phương tiện hoặc thời gian cận kề.
  2. Khi phân loại rơi vào `EXPLICIT` hoặc `IMMINENT`:
     - API lập tức gắn cờ `safety.level` vào JSON phản hồi độc lập với đoạn văn bản AI sinh ra.
     - Ứng dụng Client kích hoạt giao diện cảnh báo đỏ khẩn cấp: Cung cấp nút gọi nhanh số điện thoại cấp cứu y tế Việt Nam **115**, khuyến nghị người dùng đến cơ sở y tế gần nhất và liên hệ ngay với người thân đáng tin cậy ở cạnh bên.
     - Hệ thống đưa ra thông báo minh bạch: "MindCare không tự ý gọi điện thoại hay thông báo cho bất kỳ cơ quan nào".

#### FR-AI-04: Trò chuyện Tương tác với AI qua REST & WebSocket
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Xử lý**: 
  1. Hỗ trợ 2 phương thức giao tiếp: Endpoint REST `POST /api/ai/chat` và kênh WebSocket `/ws/ai`.
  2. Người dùng có thể truyền `conversationId` để tiếp tục hội thoại hoặc bỏ trống để hệ thống tự tạo cuộc trò chuyện mới. Khi có `conversationId`, hệ thống nạp tối đa 6 tin nhắn gần nhất làm ngữ cảnh.
  3. Mô hình AI (`gemini-3.1-flash-lite`) sinh câu trả lời kèm trích dẫn `[Nguồn N]` tương ứng với tài liệu đã truy xuất. Kiểm tra tự động xác minh số citation và thử sửa một lần trước khi từ chối. Việc nguồn có thực sự hỗ trợ từng nhận định cần đánh giá theo rubric; kiểm tra số citation không chứng minh groundedness.
  4. Lưu trữ lịch sử tin nhắn vào `ai_schema.ai_conversations` và `ai_schema.ai_conversation_messages`. Tự động dọn dẹp các cuộc hội thoại không hoạt động sau 365 ngày thông qua tác vụ định kỳ.
- **Output**: Một câu trả lời hoàn chỉnh kèm danh sách nguồn (`sources`), đối tượng an toàn (`safety`) và `conversationId`. Khi Gemini/embedding gặp lỗi nhà cung cấp, phản hồi dự phòng vẫn giữ `safety` nếu hạ tầng ứng dụng và lưu trữ hoạt động.

---

### 4.7 Phân hệ Kế hoạch Tự chăm sóc & Bookmarks (Self-Care Plan & Bookmarks)

#### FR-CARE-01: Quản lý Kế hoạch Tự chăm sóc hàng tuần
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Quy tắc**: Mỗi người dùng tại một thời điểm chỉ có tối đa một kế hoạch tự chăm sóc hiện hành. Thao tác cập nhật kế hoạch là thay thế toàn bộ (Full replacement).
- **Input**: `goal` (Mục tiêu thuộc danh mục đóng: `GIẢM CĂNG THẲNG`, `CẢI THIỆN GIẤC NGỦ`, `QUẢN LÝ LO ÂU`, `XÂY DỰNG CÂN BẰNG`), danh sách từ 1 đến 5 hoạt động (`activities: [{ activityCode, title, targetDaysPerWeek }]`).
- **Xử lý**: Lưu trữ vào `emotion_schema.self_care_plans` và `emotion_schema.self_care_activities`.
- **Output**: HTTP 200 OK / 201 Created.

#### FR-CARE-02: Điểm danh Hoàn thành Hoạt động Tự chăm sóc (Daily Completion)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: `activityId`, `completedOn` (Ngày hoàn thành định dạng `YYYY-MM-DD`).
- **Xử lý**: 
  1. Xác thực hoạt động thuộc kế hoạch của người dùng hiện tại.
  2. Người dùng chỉ được phép điểm danh cho các ngày nằm trong **tuần hiện tại** (tính từ thứ Hai đến Chủ Nhật của tuần đó). Không cho phép điểm danh ngược cho các tuần đã qua trong quá khứ.
  3. Đảm bảo tính duy nhất: Mỗi hoạt động chỉ có tối đa 1 bản ghi hoàn thành trong một ngày.
- **API**:
  - `POST /api/v1/self-care-plan/activities/{id}/completions`: Đánh dấu hoàn thành.
  - `DELETE /api/v1/self-care-plan/activities/{id}/completions/{date}`: Hủy đánh dấu.
- **Output**: HTTP 200 OK kèm tỷ lệ phần trăm hoàn thành kế hoạch trong tuần.

#### FR-CARE-03: Đánh dấu & Lưu trữ Nội dung Yêu thích (Bookmarks)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: `targetType` (`ASSESSMENT` hoặc `SELF_CARE_CONTENT`), `targetId` (Mã định danh nội dung).
- **Xử lý**: Lưu bản ghi vào `auth_schema.bookmarks`. Đảm bảo cặp `(user_id, target_type, target_id)` là duy nhất.
- **API**:
  - `POST /api/v1/bookmarks`: Thêm bookmark.
  - `GET /api/v1/bookmarks`: Xem danh sách bookmark đã lưu.
  - `DELETE /api/v1/bookmarks/{id}`: Bỏ bookmark.
- **Output**: HTTP 200 OK / 204 No Content.

---

### 4.8 Phân hệ Nhắc nhở & Thiết bị Nhận thông báo (Reminders & Push Devices)

#### FR-NOTIF-01: Cài đặt Tùy chọn Nhắc nhở Cá nhân (Reminder Preferences)
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Input**: `reminderType` (`DAILY_CHECK_IN` hoặc `SELF_CARE`), `enabled` (True/False), `localTime` (Giờ nhắc định dạng `HH:mm`), `timezone` (Tên múi giờ IANA, ví dụ: `Asia/Ho_Chi_Minh`).
- **Xử lý**: Lưu trữ vào `auth_schema.reminder_preferences`. Mỗi người dùng chỉ có tối đa một cấu hình duy nhất cho từng loại nhắc nhở.
- **Output**: HTTP 200 OK.

#### FR-NOTIF-02: Đăng ký Token Thiết bị Nhận Thông báo Đẩy (Push Devices Registry)
- **Actor**: Ứng dụng di động của người dùng (`ROLE_USER`).
- **Input**: `installationId` (UUID duy nhất theo bản cài đặt app), `pushToken` (Token do Firebase Cloud Messaging hoặc Apple Push Notification Service cấp), `platform` (`ANDROID` hoặc `IOS`).
- **Xử lý**: 
  1. Kiểm tra ràng buộc duy nhất theo `(user_id, installation_id)` và duy nhất theo `pushToken`.
  2. Cập nhật `last_seen_at = CURRENT_TIMESTAMP`, `enabled = TRUE`.
- **Output**: HTTP 200 OK.

#### FR-NOTIF-03: Truyền phát Thông báo Tức thời qua WebSocket
- **Actor**: Người dùng đã xác thực (`ROLE_USER`).
- **Kênh kết nối**: `/ws/notifications?access_token={jwt}`.
- **Xử lý**: Khi hệ thống phát sinh thông báo mới (nhắc nhở check-in đến giờ, thông báo an toàn), WebSocket Handler đẩy tin nhắn dạng JSON trực tiếp xuống phiên client đang kết nối mở.
- **Output**: Sự kiện đẩy Real-time về giao diện Web và Mobile.

---

### 4.9 Phân hệ Quản trị Hệ thống (Admin Management)

#### FR-ADM-01: Quản lý Danh sách & Trạng thái Người dùng
- **Actor**: Quản trị viên (`ROLE_ADMIN`).
- **Chức năng**:
  - `GET /api/auth/admin/users`: Xem danh sách toàn bộ người dùng hệ thống có phân trang, lọc theo vai trò và trạng thái hoạt động.
  - `PATCH /api/auth/admin/users/{id}/status`: Khóa hoặc kích hoạt lại tài khoản người dùng vi phạm tiêu chuẩn cộng đồng.

#### FR-ADM-02: Quản trị Vòng đời Bài Đánh giá Tâm lý (Assessment Lifecycle Management)
- **Actor**: Quản trị viên (`ROLE_ADMIN`).
- **Quy tắc vòng đời**: `DRAFT` $\rightarrow$ `PUBLISHED` $\rightarrow$ `ARCHIVED`.
- **Các chức năng chi tiết**:
  1. **Tạo bản nháp mới (`POST /api/v1/admin/assessments`)**: Tạo bản nháp Version 1 (`DRAFT`) cho một mã trắc nghiệm chưa từng tồn tại. Admin chỉ gửi nội dung câu hỏi; hệ thống tự động sinh các phương án trả lời chuẩn mực theo định nghĩa đã đăng ký trong `AssessmentDefinitionRegistry`.
  2. **Xem chi tiết quản trị (`GET /api/v1/admin/assessments/{id}`)**: Xem đầy đủ câu hỏi, thứ tự, điểm số raw của phương án ở mọi trạng thái vòng đời.
  3. **Cập nhật bản nháp (`PUT /api/v1/admin/assessments/{id}`)**: Thay thế toàn bộ aggregate câu hỏi/phương án của bản nháp. Tuyệt đối cấm sửa bản ghi đã `PUBLISHED` hoặc `ARCHIVED`.
  4. **Tạo phiên bản tiếp theo (`POST /api/v1/admin/assessments/{id}:create-next-version`)**: Sao chép bản ghi đang `PUBLISHED` thành một bản ghi mới với trạng thái `DRAFT` và số version tăng lên $Max + 1$. Bản ghi cũ vẫn giữ nguyên để phục vụ người dùng hiện tại. Mỗi mã trắc nghiệm chỉ cho phép tối đa 1 bản `DRAFT` tồn tại tại một thời điểm.
  5. **Xuất bản bài đánh giá (`POST /api/v1/admin/assessments/{id}:publish`)**: Chuyển trạng thái từ `DRAFT` sang `PUBLISHED`. Yêu cầu nghiêm ngặt: Số câu hỏi phải khớp 100% định nghĩa chuẩn (PHQ-9 có đúng 9 câu, GAD-7 có đúng 7 câu, WHO-5 có đúng 5 câu), thang đo khớp tuyệt đối và mã trắc nghiệm bắt buộc phải có chính sách chấm điểm đã được phê duyệt. Tự động chuyển phiên bản đã phát hành trước đó của cùng mã sang trạng thái `ARCHIVED`.
  6. **Lưu trữ bài đánh giá (`POST /api/v1/admin/assessments/{id}:archive`)**: Chuyển bài trắc nghiệm từ `PUBLISHED` sang `ARCHIVED`. Sau khi lưu trữ, bài test không còn xuất hiện trong danh mục người dùng và không nhận bài nộp mới.

#### FR-ADM-03: Quản lý và Tái lập Chỉ mục Kho Tri thức RAG
- **Actor**: Quản trị viên (`ROLE_ADMIN`).
- **Chức năng**:
  - `POST /api/ai/documents/{id}/reindex`: Tái phân tích cú pháp, chia lại chunk và sinh lại vector nhúng cho một tài liệu cụ thể.
  - `POST /api/ai/documents/reindex-all`: Kích hoạt tiến trình quét lại toàn bộ tài liệu đang hoạt động để làm mới chỉ mục vector trong PostgreSQL.

---

## 5. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)

### 5.1 Bảo mật và Kiểm soát danh tính (Security Requirements)

- **NFR-SEC-01 (Xác thực tập trung qua Gateway)**: Tất cả request từ bên ngoài đi vào hệ thống phải đi qua API Gateway. Bộ lọc `VerifiedIdentityGlobalFilter` bắt buộc loại bỏ mọi header `X-User-Id` và `X-User-Role` do client tự gửi, gọi xác thực token JWT qua endpoint `/api/auth/me` và gắn header đã kiểm định vào downstream.
- **NFR-SEC-02 (Mã hóa dữ liệu nhạy cảm)**:
  - Mật khẩu người dùng bắt buộc băm bằng thuật toán BCrypt với độ phức tạp (work factor) tiêu chuẩn $\ge 10$.
  - Mã xác thực email OTP bắt buộc băm bằng thuật toán SHA-256 trước khi lưu trữ vào cơ sở dữ liệu.
  - Toàn bộ kết nối giữa Client và Server bắt buộc sử dụng giao thức bảo mật TLS 1.3 / HTTPS.
- **NFR-SEC-03 (Phân quyền đa tầng - Defense in Depth)**: Bên cạnh việc kiểm tra quyền hạn tại Gateway, từng Microservice nội bộ bắt buộc phải kiểm tra lại Security Context và quyền sở hữu tài nguyên (Data Ownership): Người dùng chỉ được xem/sửa/xóa dữ liệu có `user_id` trùng khớp với định danh của chính mình.
- **NFR-SEC-04 (Chính sách Ghi Log An toàn)**: Tuyệt đối không ghi vào file log máy chủ các thông tin sau: Mật khẩu dạng rõ, JWT token, mã OTP, nội dung chi tiết nhật ký cảm xúc, câu trả lời trắc nghiệm tâm lý của người dùng, hoặc các giá trị đo lường sức khỏe chi tiết. Mọi bản ghi log hệ thống bắt buộc gắn kèm `X-Correlation-Id` để phục vụ điều tra lỗi.
- **NFR-SEC-05 (Bảo vệ thông tin cấu hình)**: Nghiêm cấm lưu trữ cứng (hard-code) các chuỗi bí mật, JWT Secret Key, mật khẩu database hoặc Google Gemini API Key trong mã nguồn. Toàn bộ thông tin nhạy cảm phải được truyền thông qua biến môi trường (`.env`).

### 5.2 Hiệu năng và Giới hạn phản hồi (Performance Requirements)

- **NFR-PERF-01 (Thời gian đáp ứng API thông thường)**: Các API giao dịch thông thường (Xác thực, Ghi nhật ký cảm xúc, Nộp bài đánh giá, Đánh dấu hoàn thành tự chăm sóc) phải có thời gian phản hồi ở mức phân vị 95 (p95) dưới **500 ms** trong điều kiện tải tiêu chuẩn.
- **NFR-PERF-02 (Mục tiêu độ trễ AI)**: Mục tiêu thiết kế cho streaming tương lai là token đầu tiên dưới **1.5 giây**, hoàn tất câu trả lời dưới **4.0 giây**. Hiện triển khai phản hồi hoàn chỉnh, chưa có streaming token hoặc benchmark chứng minh đạt các mốc này. Đo p95 xử lý dịch vụ và p95 toàn luồng riêng; timeout client 120 giây là giới hạn chờ, không phải cam kết hiệu năng.
- **NFR-PERF-03 (Giới hạn tải Batch dữ liệu sức khỏe)**: API tiếp nhận dữ liệu sức khỏe từ thiết bị hỗ trợ tối đa 100 bản ghi trong một request. Thời gian xử lý kiểm tra và upsert vào cơ sở dữ liệu không vượt quá **800 ms**.
- **NFR-PERF-04 (Tối ưu hóa Truy vấn Cơ sở dữ liệu)**: Toàn bộ các bảng dữ liệu lớn (`emotion_journals`, `health_metrics`, `assessment_results`, `ai_conversation_messages`) bắt buộc phải thiết lập chỉ mục (Indexes) phức hợp và chỉ mục cục bộ (Partial Indexes) trên các trường tìm kiếm thường xuyên (`user_id`, `created_at`, `deleted_at`). Bắt buộc áp dụng phân trang Keyset/Cursor để ngăn chặn suy giảm hiệu năng khi dữ liệu tăng trưởng lớn.

### 5.3 Tính sẵn sàng và Độ tin cậy (Availability & Reliability)

- **NFR-REL-01 (Độ sẵn sàng hệ thống)**: Nền tảng hướng tới mục tiêu độ sẵn sàng đạt **99.5%** trong khung giờ hoạt động tiêu chuẩn.
- **NFR-REL-02 (Tính bất biến của dữ liệu đánh giá)**: Kết quả sàng lọc tâm lý lịch sử của người dùng không bao giờ bị tính toán lại hoặc biến đổi khi hệ thống cập nhật phiên bản bài đánh giá mới.
- **NFR-REL-03 (Khả năng chịu lỗi và Idempotency)**: Toàn bộ các API nhận dữ liệu dạng batch hoặc nộp bài test đều hỗ trợ `Idempotency-Key`. Khi có lỗi mạng dẫn đến việc Client gửi lại request (Retry), hệ thống cam kết không sinh ra dữ liệu trùng lặp.

### 5.4 Khả năng mở rộng và Bảo trì (Scalability & Maintainability)

- **NFR-MAINT-01 (Kiến trúc phi trạng thái - Stateless Services)**: Các microservices Backend không lưu trữ trạng thái phiên làm việc trong bộ nhớ cục bộ (Sessionless). Mọi thông tin trạng thái được giải mã trực tiếp từ JWT Token, cho phép mở rộng quy mô theo chiều ngang (Horizontal Pod Autoscaling) dễ dàng khi triển khai trên hạ tầng container/Kubernetes.
- **NFR-MAINT-02 (Quản lý Di chuyển Schema bằng Flyway)**: 100% thay đổi cấu trúc cơ sở dữ liệu phải được quản lý thông qua các file migration phiên bản có thứ tự (`V1__...sql`, `V2__...sql`,...). Tuyệt đối không can thiệp thủ công vào cơ sở dữ liệu trên môi trường dùng chung.

### 5.5 An toàn tâm lý và Chuẩn mực y tế (Medical Disclaimer & Ethical Guardrails)

- **NFR-USE-01 (Tuyên bố từ chối trách nhiệm y tế - Medical Disclaimer)**: Mọi kết quả hiển thị từ bài trắc nghiệm tâm lý, phân tích cảm xúc hoặc câu trả lời của Trợ lý AI đều phải hiển thị kèm thông điệp chuẩn mực: *"MindCare là nền tảng theo dõi và hỗ trợ tự chăm sóc sức khỏe tinh thần, không cung cấp chẩn đoán y khoa, không thay thế cho phác đồ điều trị chuyên nghiệp hoặc dịch vụ cấp cứu y tế."*
- **NFR-USE-02 (Ngôn từ phi định kiến - Non-stigmatizing Language)**: Tất cả thông báo, khuyến nghị tự chăm sóc và kết quả phân loại rủi ro bắt buộc phải sử dụng ngôn ngữ đồng cảm, tích cực, không phán xét và không gán nhãn bệnh lý tiêu cực lên người dùng.
- **NFR-USE-03 (Quy chuẩn Đạo đức AI - Grounded AI Guardrails)**: Trợ lý AI chỉ được phép cung cấp thông tin dựa trên kho tri thức khoa học đã qua thẩm duyệt (Tier A/B). Tuyệt đối không được tự ý bịa đặt thông tin y tế (Hallucination) và không bao giờ gợi ý các hành vi tự hại hoặc phương thức tiêu cực.

---

## 6. MÔ HÌNH CƠ SỞ DỮ LIỆU & QUY ƯỚC LƯU TRỮ

### 6.1 Chiến lược phân vùng Schema

Hệ thống triển khai 3 schema riêng biệt trên cùng một cơ sở dữ liệu PostgreSQL (`mindcare_db`):
1. `auth_schema`: Quản lý tài khoản, vai trò, bảo mật xác thực, thông báo người dùng, danh sách bookmark, thiết bị di động.
2. `emotion_schema`: Quản lý nhật ký cảm xúc, đo lường sức khỏe, danh mục bài đánh giá, câu hỏi, phương án, kết quả chấm điểm, kế hoạch tự chăm sóc và nhật ký cảnh báo nguy cơ.
3. `ai_schema`: Quản lý tài liệu tri thức, các đoạn chunk vector hóa 768 chiều, chỉ mục lai HNSW/GIN, phiên trò chuyện và lịch sử tin nhắn AI.

### 6.2 Danh mục bảng dữ liệu chi tiết

```
                           +--------------------------------------+
                           |          auth_schema.roles           |
                           |--------------------------------------|
                           | PK  id: serial                       |
                           |     name: varchar(50) [UNIQUE]       |
                           |     description: varchar(255)        |
                           +------------------+-------------------+
                                              | 1
                                              |
                                              | n
+---------------------------------------------+------------------------------------+
|                                    auth_schema.users                             |
|----------------------------------------------------------------------------------|
| PK  id: uuid                                                                     |
| FK  role_id: int ------------------------> auth_schema.roles(id)                 |
|     email: varchar(255) [UNIQUE]                                                 |
|     password_hash: varchar(255)                                                  |
|     full_name: varchar(100)                                                      |
|     phone_number: varchar(20)                                                    |
|     bio: text                                                                    |
|     avatar_url: varchar(500)                                                     |
|     is_active: boolean                                                           |
|     created_at, updated_at: timestamptz                                          |
+------+----------------------+----------------------+-----------------------+-----+
       | 1                    | 1                    | 1                     | 1
       | n                    | n                    | n                     | n
       v                      v                      v                       v
+---------------+      +----------------+     +-------------------+   +--------------------+
|  auth_schema. |      |  auth_schema.  |     |   auth_schema.    |   |    auth_schema.    |
|  email_       |      |  notifications |     |   reminder_       |   |    push_devices    |
|  verifications|      |----------------|     |   preferences     |   |--------------------|
|---------------|      | PK id: uuid    |     |-------------------|   | PK id: uuid        |
| PK id: uuid   |      |    title, msg  |     | PK id: uuid       |   |    installation_id |
|    code_hash  |      |    is_read     |     |    reminder_type  |   |    push_token      |
|    expires_at |      |    created_at  |     |    local_time, tz |   |    platform        |
+---------------+      +----------------+     +-------------------+   +--------------------+

============================================================================================

+-----------------------------------------+           +------------------------------------+
|      emotion_schema.assessments         |           |   emotion_schema.emotion_journals  |
|-----------------------------------------|           |------------------------------------|
| PK  id: uuid                            |           | PK  id: uuid                       |
|     code: varchar(50)                   |           |     user_id: uuid (Logical FK)     |
|     assessment_version: int             |           |     emotion_type: varchar(50)      |
|     status: varchar(20)                 |           |     content: text                  |
|     title: varchar(255)                 |           |     energy_level: int (1..5)       |
|     description: text                   |           |     stress_level: int (1..5)       |
|     created_at, updated_at: timestamptz |           |     sleep_quality: int (1..5)      |
|     deleted_at: timestamptz             |           |     created_at, updated_at: tz     |
+--------------------+--------------------+           |     deleted_at: timestamptz        |
                     | 1                              +------------------------------------+
                     | n
                     v
+-----------------------------------------+           +------------------------------------+
|       emotion_schema.questions          |           |   emotion_schema.health_metrics    |
|-----------------------------------------|           |------------------------------------|
| PK  id: uuid                            |           | PK  id: uuid                       |
| FK  assessment_id: uuid (Cascade Delete)|           |     user_id: uuid (Logical FK)     |
|     question_text: text                 |           |     metric_type: varchar(50)       |
|     order_index: int                    |           |     metric_value: numeric(10,2)    |
+--------------------+--------------------+           |     unit: varchar(20)              |
                     | 1                              |     source_type: varchar(50)       |
                     | n                              |     external_sample_id: varchar    |
                     v                                |     recorded_at: timestamptz       |
+-----------------------------------------+           |     start_time, end_time: tz       |
|     emotion_schema.answer_options       |           +------------------------------------+
|-----------------------------------------|
| PK  id: uuid                            |           +------------------------------------+
| FK  question_id: uuid (Cascade Delete)  |           | emotion_schema.assessment_results  |
|     option_text: varchar(255)           |           |------------------------------------|
|     score_value: int                    |           | PK  id: uuid                       |
|     order_index: int                    |           |     user_id: uuid (Logical FK)     |
+-----------------------------------------+           | FK  assessment_id: uuid            |
                                                      |     total_score: int               |
                                                      |     risk_level: varchar(50)        |
                                                      |     answers_detail: jsonb          |
                                                      |     recommendations: jsonb         |
                                                      |     created_at: timestamptz        |
                                                      +------------------------------------+

============================================================================================

+-----------------------------------------+           +------------------------------------+
|   ai_schema.knowledge_documents         |           |     ai_schema.ai_conversations     |
|-----------------------------------------|           |------------------------------------|
| PK  id: uuid                            |           | PK  id: uuid                       |
|     title: varchar(255)                 |           |     user_id: uuid (Logical FK)     |
|     source_tier: varchar(10)            |           |     title: varchar(120)            |
|     review_status: varchar(30)          |           |     created_at, updated_at: tz     |
|     is_active: boolean                  |           +-----------------+------------------+
|     created_at, updated_at: timestamptz |                             | 1
+--------------------+--------------------+                             | n
                     | 1                                                v
                     | n                                      +--------------------+
                     v                                        | ai_conversation_   |
+-----------------------------------------+                   | messages           |
|     ai_schema.knowledge_chunks          |                   |--------------------|
|-----------------------------------------|                   | PK id: uuid        |
| PK  id: uuid                            |                   | FK conversation_id |
| FK  document_id: uuid (Cascade Delete)  |                   |    role: user/asst |
|     chunk_index: int                    |                   |    content: text   |
|     content: text                       |                   |    safety_level    |
|     embedding: vector(768)              |                   |    sources_json    |
|     search_vector: tsvector (Generated) |                   |    created_at      |
+-----------------------------------------+                   +--------------------+
```

### 6.3 Ma trận phân quyền dữ liệu (RBAC Matrix)

| Tài nguyên / Nghiệp vụ | Khách (Guest) | Người dùng (`ROLE_USER`) | Quản trị viên (`ROLE_ADMIN`) |
|---|:---:|:---:|:---:|
| Đăng ký, Nhập OTP xác thực, Đăng nhập | **CRUD** | **R** | **R** |
| Xem & Cập nhật hồ sơ cá nhân | - | **CRU (Own)** | **CRU (Own)** |
| Quản lý tài khoản người dùng khác | - | - | **R, U (Status)** |
| Ghi, xem, xóa mềm nhật ký cảm xúc | - | **CRD (Own)** | - *(Không có quyền xem)* |
| Xem biểu đồ xu hướng cảm xúc | - | **R (Own)** | - *(Không có quyền xem)* |
| Xem danh mục & Làm bài trắc nghiệm | - | **CR (Own)** | - *(Không nộp bài)* |
| Quản lý phiên bản bài test (Draft/Pub/Arch) | - | - | **CRUD** |
| Đồng bộ & Xóa dữ liệu sức khỏe | - | **CRD (Own)** | - |
| Trò chuyện Trợ lý AI (RAG Chat) | - | **CRUD (Own)** | **CRUD (Own)** |
| Đăng tải, duyệt tài liệu tri thức AI | - | - | **CRUD** |
| Lập kế hoạch tự chăm sóc & Hoàn thành | - | **CRUD (Own)** | - |
| Thiết lập nhắc nhở & Lưu trữ bookmark | - | **CRUD (Own)** | - |
| Kết xuất & Xóa vĩnh viễn dữ liệu (Privacy) | - | **R, D (Own)** | - |

---

## 7. MA TRẬN TRUY VẾT YÊU CẦU (TRACEABILITY MATRIX)

| Mã Yêu cầu | Tên Chức năng | Phương thức & API Endpoint | Microservice Phụ trách | Bảng Cơ sở dữ liệu Liên quan |
|---|---|---|---|---|
| **FR-AUTH-01** | Đăng ký & Gửi Email OTP | `POST /api/auth/register` | `auth-service` | `auth_schema.users`, `email_verifications` |
| **FR-AUTH-02** | Nhập OTP kích hoạt tài khoản | `POST /api/auth/verify-code` | `auth-service` | `auth_schema.email_verifications`, `users` |
| **FR-AUTH-03** | Đăng nhập hệ thống | `POST /api/auth/login` | `auth-service` | `auth_schema.users`, `roles` |
| **FR-AUTH-04** | Cập nhật hồ sơ & Avatar | `PUT /api/auth/me`, `POST /api/auth/avatar` | `auth-service` | `auth_schema.users` |
| **FR-PRIV-01** | Xuất dữ liệu cá nhân JSON | `GET /api/auth/me/data-export` | `auth` / `emotion` / `ai` | Toàn bộ các bảng có `user_id` |
| **FR-PRIV-02** | Xóa tài khoản vĩnh viễn | `DELETE /api/auth/me/permanent` | `auth` / `emotion` / `ai` | Cascade toàn bộ các bảng liên quan |
| **FR-EMO-01** | Check-in nhật ký cảm xúc | `POST /api/v1/emotion-journals` | `emotion-service` | `emotion_schema.emotion_journals` |
| **FR-EMO-02** | Xem lịch sử nhật ký phân trang | `GET /api/v1/emotion-journals` | `emotion-service` | `emotion_schema.emotion_journals` |
| **FR-EMO-03** | Xóa mềm nhật ký (cửa sổ 15p) | `DELETE /api/v1/emotion-journals/{id}` | `emotion-service` | `emotion_schema.emotion_journals` |
| **FR-EMO-04** | Trực quan hóa xu hướng | `GET /api/v1/emotion-trends` | `emotion-service` | `emotion_schema.emotion_journals` |
| **FR-ASM-01** | Danh sách bài trắc nghiệm Public | `GET /api/v1/assessments` | `emotion-service` | `emotion_schema.assessments` |
| **FR-ASM-02** | Chi tiết bài test để làm bài | `GET /api/v1/assessments/{code}` | `emotion-service` | `assessments`, `questions`, `answer_options` |
| **FR-ASM-03** | Nộp bài test có Idempotency | `POST /api/v1/assessments/{code}/submissions` | `emotion-service` | `assessments`, `assessment_results` |
| **FR-ASM-04** | Xem lịch sử & chi tiết kết quả | `GET /api/v1/assessment-results` | `emotion-service` | `emotion_schema.assessment_results` |
| **FR-HLT-01** | Batch đồng bộ dữ liệu sức khỏe | `POST /api/v1/health-metrics/batch` | `emotion-service` | `health_metrics`, `health_metric_sync_requests` |
| **FR-HLT-02** | Quản lý quyền & Xóa dữ liệu nguồn | `DELETE /api/v1/health-metrics/sources/{type}/data` | `emotion-service` | `health_source_consents`, `health_metrics` |
| **FR-AI-01** | Quản trị tài liệu tri thức | `POST /api/ai/documents` | `ai-service` | `ai_schema.knowledge_documents`, `chunks` |
| **FR-AI-02** | Tìm kiếm lai & Hợp nhất RRF | *Truy vấn nội bộ trong luồng RAG* | `ai-service` | `ai_schema.knowledge_chunks` |
| **FR-AI-03** | Phễu an toàn khủng hoảng 4 mức | *Luồng phân loại an toàn trước khi RAG* | `ai-service` | *Cơ chế Rule-based tiếng Việt nội bộ* |
| **FR-AI-04** | Trò chuyện AI qua REST/WebSocket | `POST /api/ai/chat`, `/ws/ai` | `ai-service` | `ai_conversations`, `ai_conversation_messages`, `ai_chat_requests`, `ai_chat_rate_limits` |
| **FR-CARE-01**| Thiết lập kế hoạch tự chăm sóc | `PUT /api/v1/self-care-plan` | `emotion-service` | `self_care_plans`, `self_care_activities` |
| **FR-CARE-02**| Điểm danh hoạt động trong tuần | `POST /api/v1/self-care-plan/activities/{id}/completions`| `emotion-service` | `emotion_schema.self_care_completions` |
| **FR-CARE-03**| Quản lý Bookmarks | `POST /api/v1/bookmarks` | `auth-service` | `auth_schema.bookmarks` |
| **FR-NOTIF-01**| Cài đặt giờ nhắc nhở cá nhân | `PUT /api/v1/reminders/{type}` | `auth-service` | `auth_schema.reminder_preferences` |
| **FR-NOTIF-02**| Đăng ký Token thiết bị di động | `POST /api/v1/push-devices` | `auth-service` | `auth_schema.push_devices` |
| **FR-ADM-01** | Quản lý người dùng hệ thống | `GET /api/auth/admin/users` | `auth-service` | `auth_schema.users` |
| **FR-ADM-02** | Quản trị vòng đời bài trắc nghiệm | `POST /api/v1/admin/assessments` | `emotion-service` | `assessments`, `questions`, `answer_options` |
| **FR-ADM-03** | Tái lập chỉ mục vector RAG | `POST /api/ai/documents/reindex-all`| `ai-service` | `ai_schema.knowledge_chunks` |

---
*Tài liệu này phản ánh trung thực trạng thái triển khai kiến trúc và nghiệp vụ của dự án MindCare Monorepo. Mọi thay đổi về sau liên quan đến hợp đồng giao diện API hoặc cấu trúc bảng dữ liệu bắt buộc phải được cập nhật đồng bộ vào tài liệu SRS này.*
