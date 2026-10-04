# Cảnh báo benchmark từ smartwatch

## Phân loại mức thông báo

| Nguồn | Điều kiện | Mức hệ thống | Push |
|---|---|---|---|
| PMData | Điểm 1–2 | `INFORMATIONAL` | Không |
| PMData | Điểm 3 | `MONITOR` | Không |
| PMData | Điểm 4 | `ELEVATED` | Có |
| PMData | Điểm 5 | `HIGH` | Có |
| Benchmark giấc ngủ/bước chân | Policy bị kích hoạt | `WELLNESS` | Có, ưu tiên thấp hơn PMData 4–5 |
| Benchmark nhịp tim nghỉ | Policy bị kích hoạt | `CHECK` | Có, khuyến nghị đo lại và xem xét triệu chứng |

Mọi kết quả PMData được lưu tại `pmdata_stress_predictions`. Điểm 1–3 vẫn có lịch sử nhưng không làm phiền người dùng. Điểm 4–5 không tạo push tức thì: vào giờ `DAILY_CHECK_IN` do người dùng chọn, hệ thống chỉ mời người dùng tự check-in nếu có ba ngày liên tiếp kết thúc ở hôm qua đều ở mức 4–5. Ngày thiếu hoặc cửa sổ có điểm thấp hơn không kích hoạt lời mời theo insight. PMData là ước lượng từ wearable, không phải thang chẩn đoán lâm sàng.

## Các policy hiện hành

| Policy | Điều kiện tạo cảnh báo | Nguồn |
|---|---|---|
| `SLEEP_DURATION_CDC_ADULT_V1` | Có dữ liệu ít nhất 4/7 ngày và ít nhất 3 ngày ngủ dưới 7 giờ; chỉ áp dụng ngôn ngữ hỗ trợ cho người lớn 18–60 | https://www.cdc.gov/sleep/about/index.html |
| `STEP_DEFINED_ACTIVITY_2013_V1` | Có dữ liệu ít nhất 4/7 ngày và trung bình các ngày có dữ liệu dưới 5.000 bước/ngày | https://pubmed.ncbi.nlm.nih.gov/23438219/ |
| `AHA_RESTING_HEART_RATE_V2` | Ít nhất 2 mẫu có `details.measurementContext=RESTING` cùng thấp hơn 60 hoặc cùng cao hơn 100 bpm, và mẫu mới nhất vẫn ở cùng phía ngưỡng | https://www.heart.org/en/health-topics/high-blood-pressure/the-facts-about-high-blood-pressure/all-about-heart-rate-pulse |
| `FDA_SPO2_TYPICAL_RANGE_V1` | Ít nhất 2 mẫu dưới 95% và mẫu mới nhất cũng dưới 95% | https://www.fda.gov/consumers/consumer-updates/pulse-oximeter-basics |

## Guardrail

- Thiếu dữ liệu trả `INSUFFICIENT_DATA`, không cảnh báo.
- Không đánh giá nhịp tim hoạt động như nhịp tim lúc nghỉ.
- SpO₂ và nhịp tim cần phép đo lặp lại; thông báo yêu cầu xem xét triệu chứng và đo lại, không chẩn đoán.
- Đồng hồ/thiết bị wellness có giới hạn độ chính xác. SpO₂ 95–100% chỉ là khoảng điển hình FDA nêu cho đa số người khỏe mạnh, không phải ngưỡng cấp cứu tự động.
- Sau khi một batch health metric có thay đổi được commit, backend tự chạy các policy và tạo notification intent; mobile không phải gọi API phân tích. Cooldown là 7 ngày cho ngủ/bước chân và 24 giờ cho nhịp tim/SpO₂.
- Alert lưu policy key/version/source, observed value/unit và plan template được đề xuất để audit.

## API

- `GET /api/v1/health-metrics/benchmark-evaluations`: trả kết quả của cả bốn policy, kể cả đủ chuẩn và thiếu dữ liệu.
- `POST /api/v1/risk-alerts/analyze-health`: lưu các cảnh báo mới sau khi áp dụng cooldown.

Endpoint POST vẫn được giữ cho kiểm thử/vận hành thủ công. Luồng production thông thường được kích hoạt tự động bởi sự kiện nội bộ sau commit của `POST /api/v1/health-metrics/sync`. Alert mới được gửi sang notification capability bằng internal REST có `eventId = alertId`; notification capability chống trùng, lưu vào trung tâm thông báo, phát WebSocket và gửi Expo push nếu push được bật.

PMData được lưu qua `POST /api/v1/health-metrics/stress-predictions`; có thể đọc bản ghi gần nhất bằng `GET /api/v1/health-metrics/stress-predictions/latest`. Khi cửa sổ ba ngày đủ điều kiện, backend dùng reminder hiện có để gửi một lời hỏi thăm trung tính dẫn đến `/emotion`; nội dung không chứa điểm dự báo hay kết luận người dùng đang căng thẳng. Mobile không tự tạo local notification cho kết quả PMData, nhờ đó không có thông báo trùng.

## Đánh giá ngày vừa qua: `daily-wellness-v2.0`

API:

```http
GET /api/v1/health-metrics/daily-evaluation
    ?date=2026-09-10
    &timezone=Asia%2FHo_Chi_Minh
```

- Nếu bỏ `date`, server đánh giá ngày lịch hoàn tất gần nhất, tức hôm qua theo `timezone`.
- `status` và thông báo chính được quyết định bởi benchmark khoa học. Baseline cá nhân chỉ bổ sung ngữ cảnh và không thay đổi kết luận benchmark.
- Baseline cá nhân là trung vị của tối đa 28 ngày đứng trước ngày đánh giá; cần ít nhất 7 ngày có dữ liệu cho từng chỉ số.
- Baseline không bao gồm ngày đang đánh giá, vì vậy một ngày bất thường không tự làm thay đổi mốc dùng để đánh giá chính nó.
- Trung vị và MAD (median absolute deviation) được dùng cho nhịp tim/SpO₂ để giảm ảnh hưởng của điểm nhiễu.
- `clinicalDiagnosis` luôn là `false`. Kết quả chỉ hỗ trợ tự theo dõi và không thay thế đánh giá của nhân viên y tế.

| Chỉ số | Ngưỡng tham khảo chung | Điều kiện lệch baseline cá nhân |
|---|---|---|
| Ngủ | Dưới 7 giờ đối với người lớn 18–60 theo CDC | Thấp hơn baseline ít nhất 1 giờ **và** ít nhất 20% |
| Bước chân | Dưới 5.000 bước/ngày là chỉ số lối sống ít vận động ở người lớn theo tổng quan Tudor-Locke et al.; không phải ngưỡng chẩn đoán hay mục tiêu điều trị | Thấp hơn baseline ít nhất 1.000 bước **và** ít nhất 40% |
| Nhịp tim nghỉ | Ngoài 60–100 bpm theo AHA; có ngoại lệ do luyện tập, thuốc và tình trạng cá nhân | Lệch baseline ít nhất `max(10 bpm, 3 × MAD)` |
| SpO₂ | Ít nhất hai phép đo dưới 95% và trung vị ngày dưới 95%; 95–100% chỉ là khoảng thường gặp theo FDA | Giảm ít nhất `max(3 điểm %, 3 × MAD)` và có ít nhất hai phép đo trong ngày |

Benchmark khoa học quyết định `status`: `WITHIN_SCIENTIFIC_BENCHMARK`, `BELOW_SCIENTIFIC_BENCHMARK` hoặc `RECHECK_RECOMMENDED`. Baseline chỉ được thể hiện bằng `personalDeviationTriggered`, giá trị baseline và độ lệch.

Các tỷ lệ 20%, 40%, mức 1 giờ, 1.000 bước, 10 bpm và phép `3 × MAD` là **guardrail bổ sung có version của MindCare**, không phải ngưỡng chẩn đoán do các cơ sở y tế công bố. Chúng phải được kiểm định trên người dùng MindCare và được chuyên gia duyệt trước khi dùng cho quyết định lâm sàng.

Nguồn tham khảo:

- CDC, About Sleep: https://www.cdc.gov/sleep/about/index.html
- American Heart Association, All About Heart Rate: https://www.heart.org/en/health-topics/high-blood-pressure/the-facts-about-high-blood-pressure/all-about-heart-rate-pulse
- Tudor-Locke et al., *A step-defined sedentary lifestyle index: <5000 steps/day*: https://pubmed.ncbi.nlm.nih.gov/23438219/
- U.S. Office of Disease Prevention and Health Promotion, Physical Activity Guidelines (nguồn bổ sung): https://odphp.health.gov/our-work/nutrition-physical-activity/physical-activity-guidelines/current-guidelines/top-10-things-know
- FDA, Pulse Oximeter Basics: https://www.fda.gov/consumers/consumer-updates/pulse-oximeter-basics
