# ĐẶC TẢ CHÍNH SÁCH BENCHMARK VÀ NGUỒN MINH CHỨNG KHOA HỌC (SCIENTIFIC EVIDENCE & BENCHMARK POLICIES SPECIFICATION)
## DỰ ÁN: NỀN TẢNG THEO DÕI & TỰ CHĂM SÓC SỨC KHỎE TINH THẦN MINDCARE

---

| Thuộc tính | Chi tiết |
|---|---|
| **Mã tài liệu** | `MINDCARE-DOC-16-BENCHMARK-EVIDENCE` |
| **Thuộc phân hệ** | `emotion-service` & `ai-service` |
| **Tiêu chuẩn tham chiếu** | APA, WHO Guidelines, IEEE Std 830-1998 |
| **Phiên bản tài liệu** | 1.0.0 |
| **Trạng thái** | Ban hành chính thức làm tài liệu đối sánh kỹ thuật và nghiên cứu |

---

## 1. TỔNG QUAN VỀ KHÁI NIỆM BENCHMARK TRONG MINDCARE

Trong nền tảng **MindCare**, thuật ngữ **Benchmark** được phân định rõ ràng thành 3 phạm trù chuyên biệt nhằm tránh nhầm lẫn giữa đo lường tâm lý học lâm sàng và kiểm thử kỹ thuật phần mềm:

1. **Assessment Benchmark Policies (Chuẩn đối sánh thang đo tâm lý)**: 👉 **ĐÃ HIỆN THỰC HÓA 100% TRONG HỆ THỐNG**.
   - Là các chính sách khoa học dùng để phân dải ngưỡng điểm (score cut-offs), diễn giải mức độ triệu chứng (interpretation levels) và đưa ra khuyến nghị an toàn dựa trên các công trình nghiên cứu y học/tâm lý học quốc tế đã qua bình duyệt (peer-reviewed).
   - Được định nghĩa trong cơ sở dữ liệu (`emotion_schema.benchmark_policies`, `emotion_schema.benchmark_bands`), mã nguồn Java (`AssessmentEvidenceRegistry`, `AssessmentScoringPolicyRegistry`) và kiểm soát nghiêm ngặt bằng phiên bản bất biến.

2. **AI RAG Evaluation Benchmark (Bộ chuẩn đánh giá năng lực AI & RAG)**: 👉 **ĐÃ CÓ BỘ TIÊU CHÍ KIẾN TRÚC, ĐANG HOÀN THIỆN DATASET THỰC NGHIỆM**.
   - Bộ tiêu chuẩn đánh giá định lượng mức độ chính xác của tìm kiếm tri thức (Retrieval Recall), độ tiếp đất không ảo giác (Groundedness), độ chính xác trích dẫn (Citation Validity) và độ nhạy an toàn khủng hoảng (Crisis Routing Recall).
   - Được đặc tả trong `ai-service/docs/RAG-ARCHITECTURE.md`.

3. **Performance / Load Benchmark (Chuẩn đo tải và hiệu năng hệ thống)**: 👉 **ĐANG TRONG LỘ TRÌNH KỸ THUẬT**.
   - Các bài đo tải p95/p99 với k6/JMeter để xác định giới hạn chịu tải của HikariCP Connection Pool và tài nguyên phần cứng (được ghi nhận trong `08-development-roadmap.md`).

Tài liệu này tập trung chi tiết vào **Nhóm 1 (Assessment Benchmark Policies)** kèm toàn bộ nguồn minh chứng khoa học nguyên bản, đồng thời tóm lược tiêu chuẩn của **Nhóm 2 (AI Evaluation Benchmark)**.

---

## 2. CHI TIẾT NGUỒN MINH CHỨNG KHOA HỌC CHO CÁC THANG ĐO TÂM LÝ

Mỗi bài trắc nghiệm tâm lý trước khi được phê duyệt phát hành (`PUBLISHED`) trên MindCare bắt buộc phải đáp ứng đồng thời 3 thành tố truy vết:
- **Cấu trúc câu hỏi & phương án trả lời** (Structural Definition).
- **Chính sách tính điểm phiên bản hóa** (`ScoringPolicy`).
- **Chính sách đối sánh chuẩn & minh chứng học thuật** (`BenchmarkPolicy` & `AssessmentEvidence`).

```
+-------------------------------------------------------------------------------------------------------+
|                                    ASSESSMENT TRACEABILITY PIPELINE                                   |
|                                                                                                       |
|   +-----------------------+       +-------------------------+       +-----------------------------+   |
|   |  CÔNG TRÌNH GỐC       | ----> |  BENCHMARK & SCORING    | ----> |  BẢN GHI KẾT QUẢ NGƯỜI DÙNG |   |
|   |  (Tác giả, DOI, WHO)  |       |  (Policies & Bands)     |       |  (Snapshots & Audit Trail)  |   |
|   +-----------------------+       +-------------------------+       +-----------------------------+   |
+-------------------------------------------------------------------------------------------------------+
```

---

### 2.1. Thang đo trầm cảm PHQ-9 (Patient Health Questionnaire-9)

#### A. Nguồn minh chứng khoa học (Scientific Evidence)
* **Tên công trình**: *The PHQ-9: Validity of a Brief Depression Severity Measure*
* **Tác giả**: Kurt Kroenke, MD; Robert L. Spitzer, MD; Janet B. W. Williams, DSW
* **Tạp chí công bố**: **Journal of General Internal Medicine (JGIM)**, Tập 16, Số 9, Trang 606–613, Tháng 9 năm 2001
* **Định danh số (DOI)**: [`10.1046/j.1525-1497.2001.016009606.x`](https://doi.org/10.1046/j.1525-1497.2001.016009606.x)
* **Độ giá trị khoa học (Validation)**:
  - Thử nghiệm trên cỡ mẫu nghiên cứu thực tế gồm **6.000 bệnh nhân** (3.000 bệnh nhân tại 8 phòng khám chăm sóc sức khỏe ban đầu và 3.000 bệnh nhân tại 7 phòng khám phụ sản).
  - Độ nhạy (Sensitivity): **88%**; Độ đặc hiệu (Specificity): **88%** đối với chẩn đoán rối loạn trầm cảm nặng (Major Depressive Disorder) tại điểm cắt $\ge 10$.
  - Hệ số nhất quán nội tại (Internal Consistency - Cronbach’s $\alpha$): **0.89**.
  - Độ tin cậy test-retest: $r = 0.84$.

#### B. Khóa chính sách & Cấu hình trong hệ thống MindCare
* **Mã công cụ (`AssessmentCode`)**: `PHQ_9`
* **Khóa chính sách tính điểm (`ScoringPolicyKey`)**: `PHQ9_SCORE` (Phiên bản `1.0`)
* **Khóa chính sách Benchmark (`BenchmarkPolicyKey`)**: `PHQ9_KROENKE_2001` (Phiên bản `1.0`)
* **Loại diễn giải (`InterpretationType`)**: `SEVERITY_BANDS`
* **URL nguồn đối sánh**: `https://doi.org/10.1046/j.1525-1497.2001.016009606.x`

#### C. Bảng phân dải Benchmark (Benchmark Bands)
Thang đo gồm 9 câu hỏi, mỗi câu tính điểm từ 0 đến 3 (tổng điểm từ 0 đến 27):

| Dải điểm (Min - Max) | Cấp độ chuẩn hóa (`Level`) | Diễn giải lâm sàng theo Kroenke (2001) | Khuyến nghị hành động của MindCare |
|---|---|---|---|
| **0 – 4** | `MINIMAL` | Không có hoặc trầm cảm tối thiểu | Tiếp tục theo dõi sức khỏe tinh thần theo thời gian. |
| **5 – 9** | `MILD` | Trầm cảm mức độ nhẹ | Cân nhắc chia sẻ với người thân tin cậy hoặc cơ sở y tế phù hợp; thực hành bài tập tự chăm sóc. |
| **10 – 14** | `MODERATE` | Trầm cảm mức độ vừa | Khuyến khích tham vấn với chuyên gia tâm lý hoặc bác sĩ chuyên khoa. |
| **15 – 19** | `MODERATELY_SEVERE` | Trầm cảm mức độ nặng vừa | Nên sớm liên hệ cơ sở y tế hoặc dịch vụ sức khỏe tinh thần để được hỗ trợ chuyên nghiệp trực tiếp. |
| **20 – 27** | `SEVERE` | Trầm cảm mức độ nặng | Cần khẩn trương tìm kiếm sự trợ giúp y tế và theo dõi chặt chẽ từ người thân. |

#### D. Giám sát an toàn đặc biệt (Safety Item 9 Guardrail)
* Câu hỏi số 9 của PHQ-9: *"Có ý nghĩ rằng thà mình không còn sống hoặc muốn làm tổn thương bản thân"* được tách riêng thành tín hiệu cảnh báo độc lập (`RiskSignal`).
* **Quy tắc**: Bất kể tổng điểm thuộc dải nào (kể cả tổng điểm < 5), nếu câu số 9 có giá trị $> 0$, hệ thống tự động sinh `RiskSignal("SELF_HARM_ITEM", "PHQ9_ITEM_9", responseValue, "risk-phq9-item9-v1")` để kích hoạt giao diện cảnh báo hỗ trợ khẩn cấp.

---

### 2.2. Thang đo rối loạn lo âu lan tỏa GAD-7 (Generalized Anxiety Disorder-7)

#### A. Nguồn minh chứng khoa học (Scientific Evidence)
* **Tên công trình**: *A Brief Measure for Assessing Generalized Anxiety Disorder: The GAD-7*
* **Tác giả**: Robert L. Spitzer, MD; Kurt Kroenke, MD; Janet B. W. Williams, DSW; Bernd Löwe, MD, PhD
* **Tạp chí công bố**: **Archives of Internal Medicine**, Tập 166, Số 10, Trang 1092–1097, Tháng 5 năm 2006
* **Định danh số (DOI)**: [`10.1001/archinte.166.10.1092`](https://doi.org/10.1001/archinte.166.10.1092)
* **Độ giá trị khoa học (Validation)**:
  - Nghiên cứu xác thực trên cỡ mẫu **2.739 bệnh nhân** người lớn tại 15 phòng khám chăm sóc ban đầu tại Hoa Kỳ.
  - Điểm cắt tối ưu $\ge 10$ cho thấy Độ nhạy: **89%**, Độ đặc hiệu: **82%** đối với chẩn đoán Rối loạn lo âu lan tỏa (GAD).
  - Thang đo cũng có độ nhạy tốt trong việc sàng lọc Rối loạn hoảng sợ (Panic Disorder - 74%), Rối loạn lo âu xã hội (Social Anxiety - 72%) và PTSD (66%).
  - Hệ số Cronbach’s $\alpha$: **0.92**; Độ tin cậy test-retest (Intraclass correlation): **0.83**.

#### B. Khóa chính sách & Cấu hình trong hệ thống MindCare
* **Mã công cụ (`AssessmentCode`)**: `GAD_7`
* **Khóa chính sách tính điểm (`ScoringPolicyKey`)**: `GAD7_SCORE` (Phiên bản `1.0`)
* **Khóa chính sách Benchmark (`BenchmarkPolicyKey`)**: `GAD7_SPITZER_2006` (Phiên bản `1.0`)
* **Loại diễn giải (`InterpretationType`)**: `SEVERITY_BANDS`
* **URL nguồn đối sánh**: `https://doi.org/10.1001/archinte.166.10.1092`

#### C. Bảng phân dải Benchmark (Benchmark Bands)
Thang đo gồm 7 câu hỏi, mỗi câu tính điểm từ 0 đến 3 (tổng điểm từ 0 đến 21):

| Dải điểm (Min - Max) | Cấp độ chuẩn hóa (`Level`) | Diễn giải lâm sàng theo Spitzer (2006) | Khuyến nghị hành động của MindCare |
|---|---|---|---|
| **0 – 4** | `MINIMAL` | Lo âu tối thiểu / Bình thường | Tiếp tục theo dõi sức khỏe tinh thần theo thời gian. |
| **5 – 9** | `MILD` | Rối loạn lo âu mức độ nhẹ | Thực hành kỹ thuật thư giãn, thở chậm, bài tập Grounding 5-4-3-2-1. |
| **10 – 14** | `MODERATE` | Rối loạn lo âu mức độ vừa | Điểm cắt lâm sàng; cân nhắc trao đổi với chuyên gia tâm lý/bác sĩ. |
| **15 – 21** | `SEVERE` | Rối loạn lo âu mức độ nặng | Khuyến nghị tiếp cận chăm sóc y tế chuyên môn để có kế hoạch hỗ trợ toàn diện. |

---

### 2.3. Chỉ số sức khỏe tinh thần WHO-5 (WHO-Five Well-Being Index)

#### A. Nguồn minh chứng khoa học (Scientific Evidence)
* **Cơ quan ban hành**: **Tổ chức Y tế Thế giới (World Health Organization - WHO)**
* **Tài liệu chính thức**: *The World Health Organization-Five Well-Being Index (WHO-5)* (Bản cập nhật chuẩn hóa năm 2024)
* **Mã định danh ấn phẩm**: `WHO-UCN-MSD-MHE-2024.01`
* **Giấy phép bản quyền**: Creative Commons Attribution-NonCommercial-ShareAlike 3.0 IGO (`CC BY-NC-SA 3.0 IGO`)
* **URL chính thức**: [`https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01`](https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01)
* **Nghiên cứu nền tảng**:
  - Bech, P., Gudex, C., & Johansen, K. S. (1996). *The WHO (Ten) Well-Being Index: Validation in diabetes*.
  - Topp, C. W., Østergaard, S. D., Søndergaard, S., & Bech, P. (2015). *The WHO-5 Well-Being Index: a systematic review of the literature*. **Psychotherapy and Psychosomatics**, 84(3), 167–176 (Tổng hợp hơn 213 nghiên cứu trên toàn cầu chứng minh độ tin cậy và độ nhạy cao trong sàng lọc trầm cảm và đo lường hạnh phúc).

#### B. Khóa chính sách & Cấu hình trong hệ thống MindCare
* **Mã công cụ (`AssessmentCode`)**: `WHO_5`
* **Khóa chính sách tính điểm (`ScoringPolicyKey`)**: `WHO5_SCORE` (Phiên bản `1.0`)
* **Khóa chính sách Benchmark (`BenchmarkPolicyKey`)**: `WHO5_2024` (Phiên bản `1.0`)
* **Loại diễn giải (`InterpretationType`)**: `WELL_BEING_THRESHOLD`
* **URL nguồn đối sánh**: `https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01`

#### C. Công thức tính điểm chuẩn hóa & Bảng ngưỡng Benchmark
* Thang đo gồm 5 câu hỏi tích cực về 2 tuần gần nhất, mỗi câu từ 0 đến 5 điểm.
* Điểm thô (Raw Score): $\text{Raw} = \sum_{i=1}^{5} \text{score}_i \in [0, 25]$.
* **Điểm chuẩn hóa (Normalized Score)**: Quy đổi về thang điểm phần trăm theo chuẩn WHO:
  $$\text{Normalized Score} = \text{Raw Score} \times 4 \quad (\in [0, 100])$$

| Điểm chuẩn hóa | Cấp độ chuẩn hóa (`Level`) | Ý nghĩa theo chuẩn WHO | Khuyến nghị của MindCare |
|---|---|---|---|
| **0 – 49** | `LOW_WELL_BEING` | Mức độ khỏe mạnh tinh thần thấp (Nguy cơ trầm cảm / kiệt sức) | Cân nhắc trao đổi với một người bạn tin tưởng hoặc cơ sở y tế phù hợp. |
| **50 – 100** | `ADEQUATE_WELL_BEING` | Mức độ khỏe mạnh tinh thần đầy đủ và tích cực | Duy trì lối sống lành mạnh và nhịp sinh hoạt cân bằng. |

> **Lưu ý nguyên tắc thiết kế**: Khác với PHQ-9 hay GAD-7 (điểm càng cao thì triệu chứng càng nặng), đối với WHO-5, **điểm càng cao phản ánh trạng thái khỏe mạnh tinh thần càng tốt**. Vì vậy MindCare không so sánh trực tiếp điểm số giữa các thang đo và không tùy tiện chia nhỏ thành các dải severity tùy tiện ngoài chuẩn của WHO.

---

### 2.4. Thang đo cảm nhận căng thẳng PSS-10 (Perceived Stress Scale)

#### A. Nguồn minh chứng khoa học (Scientific Evidence)
* **Tổ chức nghiên cứu**: **Carnegie Mellon University** (Stress, Immunity, and Disease Laboratory - Sheldon Cohen et al., 1983, cập nhật hướng dẫn 2010)
* **Tài liệu tham chiếu**: *PSS Scoring Guide*
* **URL chính thức**: [`https://www.cmu.edu/dietrich/psychology/stress-immunity-disease-lab/scales/html/pssscoring.html`](https://www.cmu.edu/dietrich/psychology/stress-immunity-disease-lab/scales/html/pssscoring.html)
* **Bản chất công cụ**: Đo lường mức độ các tình huống trong cuộc sống được nhìn nhận là không thể dự đoán, không thể kiểm soát và quá tải trong 1 tháng qua.

#### B. Khóa chính sách & Cấu hình trong hệ thống MindCare
* **Mã công cụ (`AssessmentCode`)**: `PSS_10`
* **Khóa chính sách tính điểm (`ScoringPolicyKey`)**: `PSS10_SCORE` (Phiên bản `1.0`)
* **Khóa chính sách Benchmark (`BenchmarkPolicyKey`)**: `PSS10_TRACKING` (Phiên bản `1.0`)
* **Loại diễn giải (`InterpretationType`)**: `TRACKING_ONLY`
* **URL nguồn đối sánh**: `https://www.cmu.edu/dietrich/psychology/stress-immunity-disease-lab/scales/html/pssscoring.html`

#### C. Quy tắc đảo điểm & Chính sách Benchmark
* Gồm 10 câu hỏi, mỗi câu tính từ 0 đến 4 điểm.
* **Quy tắc đảo điểm bắt buộc**: Các câu mang nghĩa tích cực gồm **Câu 4, Câu 5, Câu 7 và Câu 8** phải được đảo ngược điểm trước khi tính tổng:
  $$\text{Reversed Score} = 4 - \text{Raw Answer}$$
  Các câu 1, 2, 3, 6, 9, 10 giữ nguyên điểm gốc.
* **Chính sách đối sánh `TRACKING_ONLY`**:
  - Không có dải chẩn đoán lâm sàng.
  - Cấp độ trả về cố định là `TRACKING_ONLY`.
  - Khuyến nghị: *"Dùng điểm này để theo dõi thay đổi theo thời gian; không xem đây là một chẩn đoán hoặc mức độ bệnh."*

---

### 2.5. Trạng thái quản trị đối với DASS-21 (Depression Anxiety Stress Scales)

* **Nguồn gốc**: University of New South Wales (UNSW DASS FAQ - Lovibond & Lovibond, 1995).
* **Quyết định kiến trúc (`12-assessment-benchmark-policies.md`)**:
  - Thang đo DASS-21 có cấu trúc đa chiều phức tạp và dễ gây nhầm lẫn khi người dùng tự đọc kết quả nếu thiếu hướng dẫn chuyên gia trực tiếp.
  - Do đó, trong Migration `V6__Assessment_Benchmark_Policies.sql`, MindCare đã chủ động đưa DASS-21 về trạng thái **`ARCHIVED` / `REVIEW_REQUIRED`**, ẩn khỏi toàn bộ API công khai và cấm nộp bài cho đến khi hoàn thành quy trình thẩm định chuyên môn độc lập.

---

## 3. THIẾT KẾ CƠ SỞ DỮ LIỆU & CƠ CHẾ TRUY VẾT BENCHMARK

Để đảm bảo mọi kết quả trắc nghiệm của người dùng đều có thể truy vết về tài liệu gốc và không bị sai lệch khi hệ thống cập nhật trong tương lai, cơ sở dữ liệu lưu trữ theo mô hình Audit Snapshot:

```sql
-- 1. Bảng quản lý chính sách chấm điểm
CREATE TABLE emotion_schema.scoring_policies (
    policy_key VARCHAR(100) PRIMARY KEY,
    assessment_code VARCHAR(50) NOT NULL,
    policy_version VARCHAR(30) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_scoring_policy_version UNIQUE (assessment_code, policy_version)
);

-- 2. Bảng quản lý chính sách Benchmark đối sánh
CREATE TABLE emotion_schema.benchmark_policies (
    policy_key VARCHAR(100) PRIMARY KEY,
    assessment_code VARCHAR(50) NOT NULL,
    policy_version VARCHAR(30) NOT NULL,
    interpretation_type VARCHAR(40) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_benchmark_policy_version UNIQUE (assessment_code, policy_version)
);

-- 3. Bảng phân dải điểm Benchmark
CREATE TABLE emotion_schema.benchmark_bands (
    id UUID PRIMARY KEY,
    benchmark_policy_key VARCHAR(100) NOT NULL REFERENCES emotion_schema.benchmark_policies(policy_key),
    level VARCHAR(50) NOT NULL,
    minimum_score INTEGER NOT NULL,
    maximum_score INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT ck_benchmark_band_range CHECK (minimum_score <= maximum_score),
    CONSTRAINT uq_benchmark_band_level UNIQUE (benchmark_policy_key, level)
);

-- 4. Bảng kết quả lưu giữ vết chính sách tại thời điểm nộp bài
ALTER TABLE emotion_schema.assessment_results
    ADD COLUMN normalized_score INTEGER,
    ADD COLUMN interpretation_level VARCHAR(50),
    ADD COLUMN scoring_policy_key VARCHAR(100),
    ADD COLUMN scoring_policy_version VARCHAR(30),
    ADD COLUMN benchmark_policy_key VARCHAR(100),
    ADD COLUMN benchmark_policy_version VARCHAR(30),
    ADD COLUMN risk_signals JSONB;
```

---

## 4. TIÊU CHUẨN BENCHMARK ĐÁNH GIÁ TRỢ LÝ AI RAG (AI EVALUATION GATE)

Bên cạnh benchmark tâm lý, tài liệu kiến trúc [`RAG-ARCHITECTURE.md`](file:///c:/Users/Hoviihohoho/Desktop/MINDCARE/mindcare-backend-monorepo/ai-service/docs/RAG-ARCHITECTURE.md) quy định bộ tiêu chí kiểm định chất lượng AI (AI Evaluation Benchmark) trước khi đưa vào môi trường Production:

| Chỉ số kiểm thử | Ngưỡng cam kết (Target Benchmark) | Phương pháp đo lường |
|---|---|---|
| **Retrieval Recall@5** | $\ge 0.85$ (85%) | Tỷ lệ tìm đúng đoạn tri thức liên quan trong Top-5 kết quả của thuật toán Hybrid Search (Vector + FullText RRF). |
| **Citation Validity** | $= 100\%$ | Toàn bộ số trích dẫn `[1]`, `[2]` trong câu trả lời phải trỏ đúng vào danh sách tài liệu ngữ cảnh được nạp vào prompt. |
| **Citation Precision** | $\ge 0.95$ (95%) | Kiểm định nội dung câu trích dẫn thực sự chứa luận điểm do nguồn cung cấp (đánh giá chuyên gia/rubric chuẩn). |
| **Groundedness Score** | $\ge 0.90$ (90%) | Tỷ lệ câu trả lời không xuất hiện ảo giác (hallucination) ngoài tài liệu đã kiểm duyệt. |
| **Crisis-Routing Recall** | $= 100\%$ | Tuyệt đối không bỏ sót bất kỳ thông điệp nào chứa từ khóa nguy cơ tự hại/khủng hoảng (định tuyến ngay sang chế độ bảo vệ khẩn cấp 115). |
| **Độ trễ phản hồi (p95)** | $\le 2.5\text{s}$ (REST) / streaming tức thời qua WebSocket | Đo đạc thời gian từ lúc nhận câu hỏi đến khi stream chunk đầu tiên từ Gemini. |

---

## 5. RANH GIỚI NGHIỆP VỤ & TUYÊN BỐ PHÁP LÝ (MEDICAL DISCLAIMER)

Căn cứ theo văn kiện [`BUSINESS_SCOPE.md`](file:///c:/Users/Hoviihohoho/Desktop/MINDCARE/mindcare-backend-monorepo/BUSINESS_SCOPE.md) và Điều 5.5 trong [`SRS.md`](file:///c:/Users/Hoviihohoho/Desktop/MINDCARE/mindcare-backend-monorepo/SRS.md):

> **Tuyên bố quan trọng**:
> 1. Toàn bộ các thang đo (PHQ-9, GAD-7, WHO-5, PSS-10) và các dải Benchmark đối sánh trong hệ thống MindCare **chỉ mang tính chất tự sàng lọc sơ bộ (Self-Screening), theo dõi diễn tiến cảm xúc cá nhân và cung cấp thông tin tham khảo**.
> 2. Hệ thống **tuyệt đối không đưa ra chẩn đoán y khoa (Medical Diagnosis)**, không đưa ra phác đồ điều trị, không thay thế cho việc thăm khám lâm sàng trực tiếp bởi bác sĩ tâm thần hoặc chuyên gia tâm lý có chứng chỉ hành nghề.
> 3. Trợ lý AI và thuật toán phân loại điểm số của MindCare không tự động điều động phương tiện cấp cứu, không cam kết xử lý khủng hoảng thay cho các tổng đài y tế khẩn cấp công cộng (115 tại Việt Nam).
