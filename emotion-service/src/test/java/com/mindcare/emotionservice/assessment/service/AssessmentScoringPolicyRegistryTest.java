package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentScoringPolicyRegistryTest {

    private final AssessmentScoringPolicyRegistry registry = new AssessmentScoringPolicyRegistry();

    @Test
    void phq9BoundaryScoresUseApprovedBands() {
        assertEquals("NORMAL", registry.score(AssessmentCode.PHQ_9, 4).riskLevel());
        assertEquals("MILD", registry.score(AssessmentCode.PHQ_9, 5).riskLevel());
        assertEquals("MODERATE", registry.score(AssessmentCode.PHQ_9, 10).riskLevel());
        assertEquals("SEVERE", registry.score(AssessmentCode.PHQ_9, 15).riskLevel());
        assertEquals("EXTREME", registry.score(AssessmentCode.PHQ_9, 20).riskLevel());
    }

    @Test
    void gad7BoundaryScoresUseApprovedBands() {
        assertEquals("NORMAL", registry.score(AssessmentCode.GAD_7, 4).riskLevel());
        assertEquals("MILD", registry.score(AssessmentCode.GAD_7, 5).riskLevel());
        assertEquals("MODERATE", registry.score(AssessmentCode.GAD_7, 10).riskLevel());
        assertEquals("SEVERE", registry.score(AssessmentCode.GAD_7, 15).riskLevel());
    }

    @Test
    void who5BoundaryScoresTreatHigherWellbeingAsLowerRisk() {
        assertEquals("MODERATE", registry.score(AssessmentCode.WHO_5, 0).riskLevel());
        assertEquals("MODERATE", registry.score(AssessmentCode.WHO_5, 12).riskLevel());
        assertEquals("NORMAL", registry.score(AssessmentCode.WHO_5, 13).riskLevel());
        assertEquals("NORMAL", registry.score(AssessmentCode.WHO_5, 25).riskLevel());
    }
}
