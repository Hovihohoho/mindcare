package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class AssessmentScoringPolicyRegistry {

    private static final String SCREENING_NOTICE =
            "Kết quả chỉ mang tính sàng lọc và hỗ trợ, không phải chẩn đoán y khoa.";

    public boolean supports(AssessmentCode assessmentCode) {
        return assessmentCode == AssessmentCode.PHQ_9 || assessmentCode == AssessmentCode.GAD_7;
    }

    public ScoringOutcome score(AssessmentCode assessmentCode, int totalScore) {
        if (assessmentCode == null) {
            throw unsupported(null);
        }
        String level = switch (assessmentCode) {
            case PHQ_9 -> phq9Level(totalScore);
            case GAD_7 -> gad7Level(totalScore);
            default -> throw unsupported(assessmentCode);
        };
        return new ScoringOutcome(
                level,
                assessmentCode.value().toLowerCase(Locale.ROOT) + "-scoring-v1",
                SCREENING_NOTICE,
                recommendations(level)
        );
    }

    private String phq9Level(int score) {
        if (score < 0 || score > 27) {
            throw invalidScore();
        }
        if (score < 5) return "NORMAL";
        if (score < 10) return "MILD";
        if (score < 15) return "MODERATE";
        if (score < 20) return "SEVERE";
        return "EXTREME";
    }

    private String gad7Level(int score) {
        if (score < 0 || score > 21) {
            throw invalidScore();
        }
        if (score < 5) return "NORMAL";
        if (score < 10) return "MILD";
        if (score < 15) return "MODERATE";
        return "SEVERE";
    }

    private List<String> recommendations(String level) {
        return switch (level) {
            case "NORMAL" -> List.of("Tiếp tục theo dõi sức khỏe tinh thần và duy trì thói quen hỗ trợ bản thân.");
            case "MILD" -> List.of("Theo dõi thay đổi theo thời gian và cân nhắc trao đổi với người bạn tin tưởng.");
            case "MODERATE" -> List.of("Cân nhắc trao đổi với chuyên gia sức khỏe tinh thần để được đánh giá phù hợp.");
            case "SEVERE", "EXTREME" -> List.of(
                    "Nên sớm liên hệ chuyên gia sức khỏe tinh thần hoặc cơ sở y tế để được hỗ trợ trực tiếp."
            );
            default -> List.of();
        };
    }

    private InvalidRequestException unsupported(AssessmentCode assessmentCode) {
        return new InvalidRequestException(
                "SCORING_POLICY_NOT_CONFIGURED",
                "No approved scoring policy is configured for " + assessmentCode
        );
    }

    private InvalidRequestException invalidScore() {
        return new InvalidRequestException("INVALID_ASSESSMENT_SCORE", "Calculated score is outside policy range");
    }

    public record ScoringOutcome(
            String riskLevel,
            String ruleVersion,
            String screeningNotice,
            List<String> recommendations
    ) {
        public ScoringOutcome {
            recommendations = List.copyOf(recommendations);
        }
    }
}
