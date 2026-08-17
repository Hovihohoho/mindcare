package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AssessmentScoringPolicyRegistry {
    private static final String SCREENING_NOTICE =
            "Kết quả chỉ mang tính sàng lọc và hỗ trợ, không phải chẩn đoán y khoa.";

    public boolean supports(AssessmentCode code) {
        return code == AssessmentCode.PHQ_9 || code == AssessmentCode.GAD_7
                || code == AssessmentCode.WHO_5 || code == AssessmentCode.PSS_10;
    }

    public ScoringOutcome score(AssessmentCode code, List<Integer> rawAnswers) {
        if (code == null || rawAnswers == null) throw unsupported(code);
        int rawScore = switch (code) {
            case PSS_10 -> pss10Score(rawAnswers);
            case PHQ_9, GAD_7, WHO_5 -> rawAnswers.stream().mapToInt(Integer::intValue).sum();
            default -> throw unsupported(code);
        };
        return switch (code) {
            case PHQ_9 -> outcome(rawScore, null, phq9Level(rawScore), "PHQ9_SCORE", "PHQ9_KROENKE_2001",
                    phq9Signals(rawAnswers));
            case GAD_7 -> outcome(rawScore, null, gad7Level(rawScore), "GAD7_SCORE", "GAD7_SPITZER_2006", List.of());
            case WHO_5 -> {
                validateRange(rawScore, 25);
                int normalized = rawScore * 4;
                yield outcome(rawScore, normalized, normalized < 50 ? "LOW_WELL_BEING" : "ADEQUATE_WELL_BEING",
                        "WHO5_SCORE", "WHO5_2024", List.of());
            }
            case PSS_10 -> outcome(rawScore, null, "TRACKING_ONLY", "PSS10_SCORE", "PSS10_TRACKING", List.of());
            default -> throw unsupported(code);
        };
    }

    /** Compatibility helper for callers/tests that only exercise additive scales. */
    public ScoringOutcome score(AssessmentCode code, int totalScore) {
        int count = switch (code) { case PHQ_9 -> 9; case GAD_7 -> 7; case WHO_5 -> 5; default -> 0; };
        if (count == 0) throw unsupported(code);
        validateRange(totalScore, code == AssessmentCode.PHQ_9 ? 27 : code == AssessmentCode.GAD_7 ? 21 : 25);
        List<Integer> answers = new java.util.ArrayList<>(java.util.Collections.nCopies(count, 0));
        for (int index = 0, remaining = totalScore; index < count && remaining > 0; index++) {
            int value = Math.min(code == AssessmentCode.WHO_5 ? 5 : 3, remaining);
            answers.set(index, value);
            remaining -= value;
        }
        return score(code, answers);
    }

    private ScoringOutcome outcome(int raw, Integer normalized, String level, String scoring, String benchmark,
                                   List<RiskSignal> signals) {
        return new ScoringOutcome(raw, normalized, level, scoring, "1.0", benchmark, "1.0",
                SCREENING_NOTICE, recommendations(level), signals);
    }

    private int pss10Score(List<Integer> answers) {
        requireAnswerCount(answers, 10);
        int total = 0;
        for (int index = 0; index < answers.size(); index++) {
            int value = answers.get(index);
            validateRange(value, 4);
            total += switch (index) { case 3, 4, 6, 7 -> 4 - value; default -> value; };
        }
        return total;
    }

    private List<RiskSignal> phq9Signals(List<Integer> answers) {
        if (answers.size() != 9) return List.of();
        int item9 = answers.get(8);
        validateRange(item9, 3);
        return item9 > 0
                ? List.of(new RiskSignal("SELF_HARM_ITEM", "PHQ9_ITEM_9", item9, "risk-phq9-item9-v1"))
                : List.of();
    }

    private String phq9Level(int score) {
        validateRange(score, 27);
        if (score < 5) return "MINIMAL";
        if (score < 10) return "MILD";
        if (score < 15) return "MODERATE";
        if (score < 20) return "MODERATELY_SEVERE";
        return "SEVERE";
    }

    private String gad7Level(int score) {
        validateRange(score, 21);
        if (score < 5) return "MINIMAL";
        if (score < 10) return "MILD";
        if (score < 15) return "MODERATE";
        return "SEVERE";
    }

    private List<String> recommendations(String level) {
        return switch (level) {
            case "MINIMAL", "ADEQUATE_WELL_BEING" -> List.of("Tiếp tục theo dõi sức khỏe tinh thần theo thời gian.");
            case "TRACKING_ONLY" -> List.of("Dùng điểm này để theo dõi thay đổi theo thời gian; không xem đây là một chẩn đoán hoặc mức độ bệnh.");
            case "LOW_WELL_BEING", "MILD", "MODERATE" -> List.of("Cân nhắc trao đổi với một người bạn tin tưởng hoặc cơ sở y tế phù hợp.");
            case "MODERATELY_SEVERE", "SEVERE" -> List.of("Nên sớm liên hệ cơ sở y tế hoặc dịch vụ sức khỏe tinh thần để được hỗ trợ trực tiếp.");
            default -> List.of();
        };
    }

    private void requireAnswerCount(List<Integer> answers, int expected) {
        if (answers.size() != expected) throw invalidScore();
    }

    private void validateRange(int score, int maximum) {
        if (score < 0 || score > maximum) throw invalidScore();
    }

    private InvalidRequestException unsupported(AssessmentCode code) {
        return new InvalidRequestException("SCORING_POLICY_NOT_CONFIGURED", "No approved scoring policy is configured for " + code);
    }

    private InvalidRequestException invalidScore() {
        return new InvalidRequestException("INVALID_ASSESSMENT_SCORE", "Calculated score is outside policy range");
    }

    public record RiskSignal(String type, String reasonCode, Integer responseValue, String ruleVersion) {}

    public record ScoringOutcome(Integer rawScore, Integer normalizedScore, String interpretationLevel,
            String scoringPolicyKey, String scoringPolicyVersion, String benchmarkPolicyKey,
            String benchmarkPolicyVersion, String screeningNotice, List<String> recommendations,
            List<RiskSignal> riskSignals) {
        public ScoringOutcome { recommendations = List.copyOf(recommendations); riskSignals = List.copyOf(riskSignals); }
        public String riskLevel() { return interpretationLevel; }
        public String ruleVersion() { return scoringPolicyKey + "-" + scoringPolicyVersion; }
    }
}
