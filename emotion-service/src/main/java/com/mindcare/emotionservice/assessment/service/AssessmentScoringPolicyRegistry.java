package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AssessmentScoringPolicyRegistry {
    private static final String SCREENING_NOTICE =
            "Kết quả chỉ mang tính sàng lọc và hỗ trợ, không phải chẩn đoán y khoa.";

    public boolean supports(AssessmentCode code) {
        return code == AssessmentCode.PHQ_9
                || code == AssessmentCode.GAD_7
                || code == AssessmentCode.WHO_5;
    }

    public ScoringOutcome score(AssessmentCode code, int totalScore) {
        if (code == null) throw unsupported(null);
        String level = switch (code) {
            case PHQ_9 -> phq9Level(totalScore);
            case GAD_7 -> gad7Level(totalScore);
            case WHO_5 -> who5Level(totalScore);
            default -> throw unsupported(code);
        };
        return new ScoringOutcome(
                level,
                code.value().toLowerCase(Locale.ROOT) + "-scoring-v1",
                SCREENING_NOTICE,
                recommendations(level));
    }

    private String phq9Level(int score) {
        validateRange(score, 27);
        if (score < 5) return "NORMAL";
        if (score < 10) return "MILD";
        if (score < 15) return "MODERATE";
        if (score < 20) return "SEVERE";
        return "EXTREME";
    }

    private String gad7Level(int score) {
        validateRange(score, 21);
        if (score < 5) return "NORMAL";
        if (score < 10) return "MILD";
        if (score < 15) return "MODERATE";
        return "SEVERE";
    }

    private String who5Level(int score) {
        validateRange(score, 25);
        // Raw score 0..25; a raw score <= 12 corresponds to < 50 on the
        // commonly reported 0..100 scale. Do not invent additional severity bands.
        return score >= 13 ? "NORMAL" : "MODERATE";
    }

    private void validateRange(int score, int maximum) {
        if (score < 0 || score > maximum) throw invalidScore();
    }

    private List<String> recommendations(String level) {
        return switch (level) {
            case "NORMAL" -> List.of(
                    "Tiếp tục theo dõi sức khỏe tinh thần và duy trì thói quen hỗ trợ bản thân.");
            case "MILD" -> List.of(
                    "Theo dõi thay đổi theo thời gian và cân nhắc trao đổi với người bạn tin tưởng.");
            case "MODERATE" -> List.of(
                    "Cân nhắc trao đổi với chuyên gia sức khỏe tinh thần để được đánh giá phù hợp.");
            case "SEVERE", "EXTREME" -> List.of(
                    "Nên sớm liên hệ chuyên gia sức khỏe tinh thần hoặc cơ sở y tế để được hỗ trợ trực tiếp.");
            default -> List.of();
        };
    }

    private InvalidRequestException unsupported(AssessmentCode code) {
        return new InvalidRequestException(
                "SCORING_POLICY_NOT_CONFIGURED",
                "No approved scoring policy is configured for " + code);
    }

    private InvalidRequestException invalidScore() {
        return new InvalidRequestException(
                "INVALID_ASSESSMENT_SCORE", "Calculated score is outside policy range");
    }

    public record ScoringOutcome(
            String riskLevel,
            String ruleVersion,
            String screeningNotice,
            List<String> recommendations) {
        public ScoringOutcome {
            recommendations = List.copyOf(recommendations);
        }
    }
}
