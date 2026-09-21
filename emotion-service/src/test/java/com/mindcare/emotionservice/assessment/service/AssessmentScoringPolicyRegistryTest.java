package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentScoringPolicyRegistryTest {

    private final AssessmentScoringPolicyRegistry registry = new AssessmentScoringPolicyRegistry();

    @Test
    void phq9BoundaryScoresUseApprovedBands() {
        assertEquals("MINIMAL", registry.score(AssessmentCode.PHQ_9, 4).interpretationLevel());
        assertEquals("MILD", registry.score(AssessmentCode.PHQ_9, 5).riskLevel());
        assertEquals("MODERATE", registry.score(AssessmentCode.PHQ_9, 10).riskLevel());
        assertEquals("MODERATELY_SEVERE", registry.score(AssessmentCode.PHQ_9, 15).interpretationLevel());
        assertEquals("SEVERE", registry.score(AssessmentCode.PHQ_9, 20).interpretationLevel());
    }

    @Test
    void gad7BoundaryScoresUseApprovedBands() {
        assertEquals("MINIMAL", registry.score(AssessmentCode.GAD_7, 4).interpretationLevel());
        assertEquals("MILD", registry.score(AssessmentCode.GAD_7, 5).riskLevel());
        assertEquals("MODERATE", registry.score(AssessmentCode.GAD_7, 10).riskLevel());
        assertEquals("SEVERE", registry.score(AssessmentCode.GAD_7, 15).riskLevel());
    }

    @Test
    void who5BoundaryScoresTreatHigherWellbeingAsLowerRisk() {
        assertEquals("LOW_WELL_BEING", registry.score(AssessmentCode.WHO_5, 0).interpretationLevel());
        assertEquals("LOW_WELL_BEING", registry.score(AssessmentCode.WHO_5, 12).interpretationLevel());
        assertEquals("ADEQUATE_WELL_BEING", registry.score(AssessmentCode.WHO_5, 13).interpretationLevel());
        assertEquals(100, registry.score(AssessmentCode.WHO_5, 25).normalizedScore());
    }

    @Test
    void pss10ReversesItemsFourFiveSevenAndEightWithoutSeverityBand() {
        var result = registry.score(AssessmentCode.PSS_10, java.util.List.of(0, 1, 2, 3, 4, 0, 1, 2, 3, 4));
        assertEquals(16, result.rawScore());
        assertEquals("TRACKING_ONLY", result.interpretationLevel());
    }

    @Test
    void phq9ItemNineCreatesRiskSignalIndependentlyFromTotalSeverity() {
        var result = registry.score(AssessmentCode.PHQ_9, java.util.List.of(0, 0, 0, 0, 0, 0, 0, 0, 1));
        assertEquals("MINIMAL", result.interpretationLevel());
        assertEquals("SELF_HARM_ITEM", result.riskSignals().get(0).type());
    }
}
