package com.mindcare.emotionservice.healthmetric.service;

import com.mindcare.emotionservice.healthmetric.dto.HealthBenchmarkEvaluation;
import com.mindcare.emotionservice.healthmetric.dto.HealthBenchmarkSnapshot;
import com.mindcare.emotionservice.healthmetric.dto.DailyHealthEvaluationResponse;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthBenchmarkServiceImpl implements HealthBenchmarkService {
    static final String CDC_SLEEP_URL = "https://www.cdc.gov/sleep/about/index.html";
    static final String STEP_INDEX_URL = "https://pubmed.ncbi.nlm.nih.gov/23438219/";
    static final String AHA_HEART_RATE_URL = "https://www.heart.org/en/health-topics/high-blood-pressure/the-facts-about-high-blood-pressure/all-about-heart-rate-pulse";
    static final String FDA_SPO2_URL = "https://www.fda.gov/consumers/consumer-updates/pulse-oximeter-basics";
    static final String DAILY_RULE_VERSION = "daily-wellness-v2.0";
    static final int PERSONAL_BASELINE_DAYS = 28;
    static final int MINIMUM_BASELINE_DAYS = 7;

    private final HealthMetricService healthMetricService;
    private final Clock clock;

    @Override
    public List<HealthBenchmarkEvaluation> evaluate(UUID userId) {
        HealthBenchmarkSnapshot snapshot = healthMetricService.getBenchmarkSnapshot(userId, 7);
        return List.of(
                sleep(snapshot),
                steps(snapshot),
                restingHeartRate(snapshot),
                oxygenSaturation(snapshot)
        );
    }

    @Override
    public DailyHealthEvaluationResponse evaluateDay(UUID userId, LocalDate requestedDate, ZoneId timezone) {
        if (userId == null) throw new InvalidRequestException("INVALID_USER_ID", "userId is required");
        if (timezone == null) throw new InvalidRequestException("INVALID_TIMEZONE", "timezone is required");
        LocalDate yesterday = OffsetDateTime.now(clock).atZoneSameInstant(timezone).toLocalDate().minusDays(1);
        LocalDate evaluationDate = requestedDate == null ? yesterday : requestedDate;
        if (evaluationDate.isAfter(yesterday)) {
            throw new InvalidRequestException(
                    "INCOMPLETE_HEALTH_DAY",
                    "date must be a completed local calendar day"
            );
        }

        HealthBenchmarkSnapshot snapshot = healthMetricService.getBenchmarkSnapshot(
                userId, evaluationDate, PERSONAL_BASELINE_DAYS + 1, timezone);
        HealthBenchmarkSnapshot.DailySummary target = snapshot.dailySummaries().stream()
                .filter(day -> evaluationDate.equals(day.date()))
                .findFirst()
                .orElse(new HealthBenchmarkSnapshot.DailySummary(evaluationDate, null, null));
        List<HealthBenchmarkSnapshot.DailySummary> baselineDays = snapshot.dailySummaries().stream()
                .filter(day -> day.date().isBefore(evaluationDate))
                .toList();

        return new DailyHealthEvaluationResponse(
                evaluationDate,
                timezone.getId(),
                DAILY_RULE_VERSION,
                PERSONAL_BASELINE_DAYS,
                false,
                "Kết luận chính theo benchmark khoa học; baseline cá nhân chỉ bổ sung. Đây không phải chẩn đoán y khoa.",
                List.of(
                        dailySleep(target.sleepHours(), baselineDays),
                        dailySteps(target.steps(), baselineDays),
                        dailyRestingHeartRate(snapshot.restingHeartRates(), evaluationDate, timezone),
                        dailyOxygenSaturation(snapshot.oxygenSaturations(), evaluationDate, timezone)
                )
        );
    }

    private DailyHealthEvaluationResponse.Signal dailySleep(
            BigDecimal observed,
            List<HealthBenchmarkSnapshot.DailySummary> history
    ) {
        List<BigDecimal> baselineValues = history.stream()
                .map(HealthBenchmarkSnapshot.DailySummary::sleepHours)
                .filter(value -> value != null)
                .toList();
        BigDecimal baseline = median(baselineValues);
        if (observed == null) {
            return dailySignal("DAILY_SLEEP_CDC_PRIMARY_V2", "SLEEP_DURATION", "NO_DATA",
                    List.of("NO_TARGET_DAY_DATA"), null, "h", 0, baseline, baselineValues.size(), false, false,
                    "CDC: người lớn 18–60 tuổi nên ngủ từ 7 giờ mỗi ngày.",
                    "Không có dữ liệu giấc ngủ cho ngày được đánh giá.", "CDC - About Sleep", CDC_SLEEP_URL);
        }
        boolean general = observed.compareTo(BigDecimal.valueOf(7)) < 0;
        boolean personal = baselineReady(baselineValues)
                && baseline.subtract(observed).compareTo(BigDecimal.ONE) >= 0
                && observed.compareTo(baseline.multiply(BigDecimal.valueOf(0.80))) <= 0;
        List<String> reasons = new ArrayList<>();
        if (general) reasons.add("SLEEP_BELOW_7_HOURS");
        if (personal) reasons.add("SLEEP_AT_LEAST_20_PERCENT_AND_1_HOUR_BELOW_BASELINE");
        String status = general ? "BELOW_SCIENTIFIC_BENCHMARK" : "WITHIN_SCIENTIFIC_BENCHMARK";
        String message = reasons.isEmpty()
                ? supplementalBaselineMessage("Thời lượng ngủ đạt benchmark khoa học đang áp dụng.", baselineValues.size())
                : general
                ? personal
                    ? "Thời lượng ngủ dưới benchmark khoa học và đồng thời thấp rõ rệt so với baseline cá nhân."
                    : "Thời lượng ngủ dưới benchmark khoa học; baseline cá nhân chỉ được dùng làm thông tin bổ sung."
                : "Thời lượng ngủ đạt benchmark khoa học nhưng thấp rõ rệt so với baseline cá nhân.";
        return dailySignal("DAILY_SLEEP_CDC_PRIMARY_V2", "SLEEP_DURATION", status, reasons,
                observed, "h", 1, baseline, baselineValues.size(), general, personal,
                "CDC: người lớn 18–60 tuổi nên ngủ từ 7 giờ mỗi ngày.", message,
                "CDC - About Sleep", CDC_SLEEP_URL);
    }

    private DailyHealthEvaluationResponse.Signal dailySteps(
            BigDecimal observed,
            List<HealthBenchmarkSnapshot.DailySummary> history
    ) {
        List<BigDecimal> baselineValues = history.stream()
                .map(HealthBenchmarkSnapshot.DailySummary::steps)
                .filter(value -> value != null)
                .toList();
        BigDecimal baseline = median(baselineValues);
        if (observed == null) {
            return dailySignal("DAILY_STEPS_5000_PRIMARY_V2", "STEP_COUNT", "NO_DATA",
                    List.of("NO_TARGET_DAY_DATA"), null, "count", 0, baseline, baselineValues.size(), false, false,
                    "Nghiên cứu tổng quan dùng dưới 5.000 bước/ngày làm chỉ số lối sống ít vận động ở người lớn; đây không phải ngưỡng chẩn đoán.",
                    "Không có dữ liệu bước chân cho ngày được đánh giá.",
                    "Tudor-Locke et al. - Step-defined sedentary lifestyle index", STEP_INDEX_URL);
        }
        boolean general = observed.compareTo(BigDecimal.valueOf(5_000)) < 0;
        boolean personal = baselineReady(baselineValues)
                && baseline.subtract(observed).compareTo(BigDecimal.valueOf(1_000)) >= 0
                && observed.compareTo(baseline.multiply(BigDecimal.valueOf(0.60))) <= 0;
        List<String> reasons = new ArrayList<>();
        if (general) reasons.add("STEPS_BELOW_5000_SEDENTARY_INDEX");
        if (personal) reasons.add("STEPS_AT_LEAST_40_PERCENT_AND_1000_BELOW_BASELINE");
        String status = general ? "BELOW_SCIENTIFIC_BENCHMARK" : "WITHIN_SCIENTIFIC_BENCHMARK";
        String message = general
                ? personal
                    ? "Số bước dưới chỉ số khoa học 5.000 bước/ngày và đồng thời giảm rõ rệt so với baseline cá nhân."
                    : "Số bước dưới chỉ số khoa học 5.000 bước/ngày; baseline cá nhân chỉ là thông tin bổ sung."
                : personal
                    ? "Số bước đạt benchmark khoa học nhưng giảm rõ rệt so với baseline cá nhân."
                    : supplementalBaselineMessage("Số bước đạt benchmark khoa học đang áp dụng.", baselineValues.size());
        return dailySignal("DAILY_STEPS_5000_PRIMARY_V2", "STEP_COUNT", status, reasons,
                observed, "count", 1, baseline, baselineValues.size(), general, personal,
                "Tổng quan Tudor-Locke et al.: dưới 5.000 bước/ngày là chỉ số lối sống ít vận động ở người lớn, không phải ngưỡng chẩn đoán.", message,
                "Tudor-Locke et al. - Step-defined sedentary lifestyle index", STEP_INDEX_URL);
    }

    private DailyHealthEvaluationResponse.Signal dailyRestingHeartRate(
            List<HealthBenchmarkSnapshot.Observation> observations,
            LocalDate evaluationDate,
            ZoneId timezone
    ) {
        Map<LocalDate, List<BigDecimal>> byDay = observationsByDay(observations, timezone);
        List<BigDecimal> targetValues = byDay.getOrDefault(evaluationDate, List.of());
        List<BigDecimal> baselineValues = dailyMediansBefore(byDay, evaluationDate);
        BigDecimal observed = median(targetValues);
        BigDecimal baseline = median(baselineValues);
        if (observed == null) {
            return dailySignal("DAILY_RESTING_HR_AHA_PRIMARY_V2", "HEART_RATE", "NO_DATA",
                    List.of("NO_TARGET_DAY_RESTING_MEASUREMENT"), null, "bpm", 0, baseline, baselineValues.size(), false, false,
                    "AHA: nhịp tim nghỉ phổ biến ở phần lớn người lớn là 60–100 bpm, có ngoại lệ như người tập luyện nhiều hoặc dùng thuốc.",
                    "Không có phép đo được đánh dấu là nhịp tim lúc nghỉ trong ngày.",
                    "American Heart Association - All About Heart Rate", AHA_HEART_RATE_URL);
        }
        boolean general = observed.compareTo(BigDecimal.valueOf(60)) < 0
                || observed.compareTo(BigDecimal.valueOf(100)) > 0;
        BigDecimal mad = medianAbsoluteDeviation(baselineValues, baseline);
        BigDecimal personalLimit = mad == null
                ? BigDecimal.TEN : BigDecimal.TEN.max(mad.multiply(BigDecimal.valueOf(3)));
        boolean personal = baselineReady(baselineValues)
                && observed.subtract(baseline).abs().compareTo(personalLimit) >= 0;
        List<String> reasons = new ArrayList<>();
        if (general) reasons.add("RESTING_HEART_RATE_OUTSIDE_60_100");
        if (personal) reasons.add("RESTING_HEART_RATE_OUTSIDE_PERSONAL_ROBUST_RANGE");
        String status = general ? "RECHECK_RECOMMENDED" : "WITHIN_SCIENTIFIC_BENCHMARK";
        return dailySignal("DAILY_RESTING_HR_AHA_PRIMARY_V2", "HEART_RATE", status, reasons,
                observed, "bpm", targetValues.size(), baseline, baselineValues.size(), general, personal,
                "AHA: nhịp tim nghỉ phổ biến ở phần lớn người lớn là 60–100 bpm.",
                general
                        ? "Nhịp tim nghỉ khác mốc chung hoặc baseline cá nhân. Hãy đo lại khi bình tĩnh và xem xét triệu chứng, thuốc và mức luyện tập."
                        : personal
                        ? "Nhịp tim nghỉ nằm trong mốc khoa học chung nhưng lệch rõ rệt so với baseline cá nhân; nên theo dõi và đo lại."
                        : supplementalBaselineMessage("Nhịp tim nghỉ nằm trong benchmark khoa học đang áp dụng.", baselineValues.size()),
                "American Heart Association - All About Heart Rate", AHA_HEART_RATE_URL);
    }

    private DailyHealthEvaluationResponse.Signal dailyOxygenSaturation(
            List<HealthBenchmarkSnapshot.Observation> observations,
            LocalDate evaluationDate,
            ZoneId timezone
    ) {
        Map<LocalDate, List<BigDecimal>> byDay = observationsByDay(observations, timezone);
        List<BigDecimal> targetValues = byDay.getOrDefault(evaluationDate, List.of());
        List<BigDecimal> baselineValues = dailyMediansBefore(byDay, evaluationDate);
        BigDecimal observed = median(targetValues);
        BigDecimal baseline = median(baselineValues);
        if (observed == null) {
            return dailySignal("DAILY_SPO2_FDA_PRIMARY_V2", "SPO2", "NO_DATA",
                    List.of("NO_TARGET_DAY_DATA"), null, "%", 0, baseline, baselineValues.size(), false, false,
                    "FDA: 95–100% là khoảng thường gặp ở phần lớn người khỏe mạnh, nhưng kết quả phải được xem cùng triệu chứng và giới hạn thiết bị.",
                    "Không có dữ liệu SpO₂ cho ngày được đánh giá.", "FDA - Pulse Oximeter Basics", FDA_SPO2_URL);
        }
        long below95 = targetValues.stream().filter(value -> value.compareTo(BigDecimal.valueOf(95)) < 0).count();
        boolean repeated = targetValues.size() >= 2;
        boolean general = repeated && below95 >= 2 && observed.compareTo(BigDecimal.valueOf(95)) < 0;
        BigDecimal mad = medianAbsoluteDeviation(baselineValues, baseline);
        BigDecimal personalLimit = mad == null
                ? BigDecimal.valueOf(3) : BigDecimal.valueOf(3).max(mad.multiply(BigDecimal.valueOf(3)));
        boolean personal = repeated && baselineReady(baselineValues)
                && baseline.subtract(observed).compareTo(personalLimit) >= 0;
        List<String> reasons = new ArrayList<>();
        if (!repeated && observed.compareTo(BigDecimal.valueOf(95)) < 0) reasons.add("SPO2_LOW_SINGLE_READING_RECHECK");
        if (general) reasons.add("SPO2_BELOW_95_REPEATED");
        if (personal) reasons.add("SPO2_DROP_OUTSIDE_PERSONAL_ROBUST_RANGE");
        String status = general || (!repeated && observed.compareTo(BigDecimal.valueOf(95)) < 0)
                ? "RECHECK_RECOMMENDED" : "WITHIN_SCIENTIFIC_BENCHMARK";
        return dailySignal("DAILY_SPO2_FDA_PRIMARY_V2", "SPO2", status, reasons,
                observed, "%", targetValues.size(), baseline, baselineValues.size(), general, personal,
                "FDA: 95–100% là khoảng thường gặp ở phần lớn người khỏe mạnh; xu hướng có thể hữu ích hơn một phép đo đơn lẻ.",
                general || (!repeated && observed.compareTo(BigDecimal.valueOf(95)) < 0)
                        ? "Hãy đo lại khi nghỉ và xem xét triệu chứng. Thiết bị wellness có thể không phải thiết bị y tế; liên hệ cơ sở y tế nếu lo ngại hoặc triệu chứng nặng hơn."
                        : personal
                        ? "SpO₂ nằm trong khoảng tham khảo chung nhưng giảm rõ rệt so với baseline cá nhân; nên tiếp tục theo dõi."
                        : supplementalBaselineMessage("SpO₂ nằm trong benchmark khoa học đang áp dụng.", baselineValues.size()),
                "FDA - Pulse Oximeter Basics", FDA_SPO2_URL);
    }

    private DailyHealthEvaluationResponse.Signal dailySignal(
            String policyKey, String metricType, String status, List<String> reasons,
            BigDecimal observed, String unit, int targetSamples, BigDecimal baseline, int baselineDays,
            boolean general, boolean personal, String generalReference, String message,
            String sourceTitle, String sourceUrl
    ) {
        BigDecimal absoluteChange = observed == null || baseline == null
                ? null : observed.subtract(baseline).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentageChange = observed == null || baseline == null || baseline.signum() == 0
                ? null : observed.subtract(baseline).multiply(BigDecimal.valueOf(100))
                .divide(baseline, 2, RoundingMode.HALF_UP);
        return new DailyHealthEvaluationResponse.Signal(
                policyKey, metricType, status, List.copyOf(reasons), scale(observed), unit, targetSamples,
                scale(baseline), baselineDays, absoluteChange, percentageChange, general, personal,
                generalReference, message, sourceTitle, sourceUrl);
    }

    private Map<LocalDate, List<BigDecimal>> observationsByDay(
            List<HealthBenchmarkSnapshot.Observation> observations,
            ZoneId timezone
    ) {
        Map<LocalDate, List<BigDecimal>> result = new LinkedHashMap<>();
        for (HealthBenchmarkSnapshot.Observation observation : observations) {
            LocalDate date = observation.recordedAt().atZoneSameInstant(timezone).toLocalDate();
            result.computeIfAbsent(date, ignored -> new ArrayList<>()).add(observation.value());
        }
        return result;
    }

    private List<BigDecimal> dailyMediansBefore(Map<LocalDate, List<BigDecimal>> byDay, LocalDate date) {
        return byDay.entrySet().stream()
                .filter(entry -> entry.getKey().isBefore(date))
                .map(entry -> median(entry.getValue()))
                .filter(value -> value != null)
                .toList();
    }

    private boolean baselineReady(List<BigDecimal> values) {
        return values.size() >= MINIMUM_BASELINE_DAYS;
    }

    private String supplementalBaselineMessage(String benchmarkMessage, int baselineDays) {
        return baselineDays < MINIMUM_BASELINE_DAYS
                ? benchmarkMessage + " Baseline bổ sung chưa hoạt động vì cần ít nhất 7 ngày dữ liệu."
                : benchmarkMessage;
    }

    private BigDecimal medianAbsoluteDeviation(List<BigDecimal> values, BigDecimal median) {
        if (median == null || values.isEmpty()) return null;
        return median(values.stream().map(value -> value.subtract(median).abs()).toList());
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return null;
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        BigDecimal value = sorted.size() % 2 == 1
                ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        return scale(value);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private HealthBenchmarkEvaluation sleep(HealthBenchmarkSnapshot snapshot) {
        var values = snapshot.dailySummaries().stream().map(HealthBenchmarkSnapshot.DailySummary::sleepHours)
                .filter(value -> value != null).toList();
        if (values.size() < 4) return insufficient("SLEEP_DURATION_CDC_ADULT_V1", "SLEEP_DURATION", "h", values.size(), CDC_SLEEP_URL,
                "CDC – About Sleep", "SLEEP_WELLNESS_V1");
        long shortNights = values.stream().filter(value -> value.compareTo(BigDecimal.valueOf(7)) < 0).count();
        BigDecimal average = average(values);
        if (shortNights >= 3) {
            return evaluation("SLEEP_DURATION_CDC_ADULT_V1", "SLEEP_DURATION", "BELOW_BENCHMARK",
                    "SLEEP_BELOW_7_HOURS_REPEATED", average, "h", values.size(),
                    "Trong 7 ngày gần đây, ít nhất 3 ngày có thời lượng ngủ dưới 7 giờ. Đây là cảnh báo hỗ trợ cho người lớn 18–60 tuổi, không phải chẩn đoán.",
                    "CDC – About Sleep", CDC_SLEEP_URL, "SLEEP_WELLNESS_V1");
        }
        return within("SLEEP_DURATION_CDC_ADULT_V1", "SLEEP_DURATION", average, "h", values.size(), CDC_SLEEP_URL,
                "CDC – About Sleep", "SLEEP_WELLNESS_V1");
    }

    private HealthBenchmarkEvaluation steps(HealthBenchmarkSnapshot snapshot) {
        var values = snapshot.dailySummaries().stream().map(HealthBenchmarkSnapshot.DailySummary::steps)
                .filter(value -> value != null).toList();
        if (values.size() < 4) return insufficient("STEP_DEFINED_ACTIVITY_2013_V1", "STEP_COUNT", "count", values.size(), STEP_INDEX_URL,
                "Tudor-Locke et al. – Step-defined sedentary lifestyle index", "LOW_ACTIVITY_MACTIVE_V1");
        BigDecimal average = average(values);
        if (average.compareTo(BigDecimal.valueOf(5_000)) < 0) {
            return evaluation("STEP_DEFINED_ACTIVITY_2013_V1", "STEP_COUNT", "BELOW_BENCHMARK",
                    "SEVEN_DAY_AVERAGE_BELOW_5000_STEPS", average, "count", values.size(),
                    "Số bước trung bình trong các ngày có dữ liệu thấp hơn 5.000 bước/ngày. Đây là chỉ số ít vận động dùng để hỗ trợ thay đổi hành vi, không phải mục tiêu điều trị.",
                    "Tudor-Locke et al. – Step-defined sedentary lifestyle index", STEP_INDEX_URL, "LOW_ACTIVITY_MACTIVE_V1");
        }
        return within("STEP_DEFINED_ACTIVITY_2013_V1", "STEP_COUNT", average, "count", values.size(), STEP_INDEX_URL,
                "Tudor-Locke et al. – Step-defined sedentary lifestyle index", "LOW_ACTIVITY_MACTIVE_V1");
    }

    private HealthBenchmarkEvaluation restingHeartRate(HealthBenchmarkSnapshot snapshot) {
        var values = snapshot.restingHeartRates().stream().map(HealthBenchmarkSnapshot.Observation::value).toList();
        if (values.size() < 2) return insufficient("AHA_RESTING_HEART_RATE_V1", "HEART_RATE", "bpm", values.size(), AHA_HEART_RATE_URL,
                "American Heart Association – All About Heart Rate", null);
        var abnormal = values.stream().filter(value -> value.compareTo(BigDecimal.valueOf(60)) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0).toList();
        BigDecimal latest = values.get(values.size() - 1);
        if (abnormal.size() >= 2) {
            return evaluation("AHA_RESTING_HEART_RATE_V1", "HEART_RATE", "RECHECK_RECOMMENDED",
                    "RESTING_HEART_RATE_OUTSIDE_60_100_REPEATED", latest, "bpm", values.size(),
                    "Có ít nhất 2 phép đo được đánh dấu là lúc nghỉ nằm ngoài khoảng 60–100 bpm. Hãy đo lại khi bình tĩnh và xem xét triệu chứng; vận động viên, thuốc và bệnh nền có thể làm thay đổi nhịp tim.",
                    "American Heart Association – All About Heart Rate", AHA_HEART_RATE_URL, null);
        }
        return within("AHA_RESTING_HEART_RATE_V1", "HEART_RATE", latest, "bpm", values.size(), AHA_HEART_RATE_URL,
                "American Heart Association – All About Heart Rate", null);
    }

    private HealthBenchmarkEvaluation oxygenSaturation(HealthBenchmarkSnapshot snapshot) {
        var values = snapshot.oxygenSaturations().stream().map(HealthBenchmarkSnapshot.Observation::value).toList();
        if (values.size() < 2) return insufficient("FDA_SPO2_TYPICAL_RANGE_V1", "SPO2", "%", values.size(), FDA_SPO2_URL,
                "FDA – Pulse Oximeter Basics", null);
        var below = values.stream().filter(value -> value.compareTo(BigDecimal.valueOf(95)) < 0).toList();
        BigDecimal latest = values.get(values.size() - 1);
        if (below.size() >= 2 && latest.compareTo(BigDecimal.valueOf(95)) < 0) {
            return evaluation("FDA_SPO2_TYPICAL_RANGE_V1", "SPO2", "RECHECK_RECOMMENDED",
                    "SPO2_BELOW_95_REPEATED", latest, "%", values.size(),
                    "Có ít nhất 2 phép đo SpO₂ dưới khoảng điển hình 95–100%. Hãy đo lại khi nghỉ, kiểm tra triệu chứng và liên hệ cơ sở y tế nếu lo ngại hoặc triệu chứng nặng hơn. Đồng hồ thông minh có thể không phải thiết bị y tế.",
                    "FDA – Pulse Oximeter Basics", FDA_SPO2_URL, null);
        }
        return within("FDA_SPO2_TYPICAL_RANGE_V1", "SPO2", latest, "%", values.size(), FDA_SPO2_URL,
                "FDA – Pulse Oximeter Basics", null);
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private HealthBenchmarkEvaluation insufficient(String key, String metric, String unit, int days, String url, String title, String plan) {
        return new HealthBenchmarkEvaluation(key, "1.0", metric, "INSUFFICIENT_DATA", "INSUFFICIENT_DATA",
                null, unit, days, 7, "Cần dữ liệu của ít nhất 4 ngày; riêng nhịp tim lúc nghỉ và SpO₂ cần ít nhất 2 phép đo.", title, url, plan);
    }

    private HealthBenchmarkEvaluation within(String key, String metric, BigDecimal value, String unit, int days, String url, String title, String plan) {
        return evaluation(key, metric, "WITHIN_BENCHMARK", "WITHIN_BENCHMARK", value, unit, days,
                "Chưa chạm ngưỡng cảnh báo của policy hiện hành.", title, url, plan);
    }

    private HealthBenchmarkEvaluation evaluation(String key, String metric, String status, String reason, BigDecimal value,
                                                  String unit, int days, String message, String title, String url, String plan) {
        return new HealthBenchmarkEvaluation(key, "1.0", metric, status, reason, value, unit, days, 7, message, title, url, plan);
    }
}
