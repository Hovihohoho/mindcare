package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.dto.AssessmentEvidenceResponse;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AssessmentEvidenceRegistry {
    private static final String LIMITATION =
            "Đây là công cụ tự đánh giá/sàng lọc, không phải chẩn đoán y khoa và không thay thế đánh giá của chuyên gia.";
    private final Map<AssessmentCode, AssessmentEvidenceResponse> evidence;

    public AssessmentEvidenceRegistry() {
        EnumMap<AssessmentCode, AssessmentEvidenceResponse> configured = new EnumMap<>(AssessmentCode.class);
        configured.put(AssessmentCode.WHO_5, new AssessmentEvidenceResponse(
                "World Health Organization",
                "The World Health Organization-Five Well-Being Index (WHO-5)",
                "https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01",
                2024,
                "WHO-5 2024",
                "CC BY-NC-SA 3.0 IGO",
                "who-5-scoring-v1",
                "Đo mức độ khỏe mạnh tinh thần tự báo cáo trong hai tuần gần đây.",
                LIMITATION));
        configured.put(AssessmentCode.PHQ_9, new AssessmentEvidenceResponse(
                "Journal of General Internal Medicine",
                "The PHQ-9: Validity of a Brief Depression Severity Measure",
                "https://doi.org/10.1046/j.1525-1497.2001.016009606.x",
                2001,
                "PHQ-9",
                "See source",
                "phq-9-scoring-v1",
                "Sàng lọc mức độ thường gặp của các triệu chứng trầm cảm.",
                LIMITATION));
        configured.put(AssessmentCode.GAD_7, new AssessmentEvidenceResponse(
                "Archives of Internal Medicine",
                "A Brief Measure for Assessing Generalized Anxiety Disorder",
                "https://doi.org/10.1001/archinte.166.10.1092",
                2006,
                "GAD-7",
                "See source",
                "gad-7-scoring-v1",
                "Sàng lọc mức độ thường gặp của các triệu chứng lo âu.",
                LIMITATION));
        evidence = Map.copyOf(configured);
    }

    public boolean supports(AssessmentCode code) {
        return evidence.containsKey(code);
    }

    public AssessmentEvidenceResponse getRequired(AssessmentCode code) {
        AssessmentEvidenceResponse result = evidence.get(code);
        if (result == null) {
            throw new InvalidRequestException(
                    "ASSESSMENT_EVIDENCE_NOT_CONFIGURED",
                    "Assessment cannot be published without traceable evidence metadata");
        }
        return result;
    }
}
