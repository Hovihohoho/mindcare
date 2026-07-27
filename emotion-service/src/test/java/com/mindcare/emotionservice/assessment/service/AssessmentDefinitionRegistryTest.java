package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentDefinitionRegistryTest {

    private final AssessmentDefinitionRegistry registry = new AssessmentDefinitionRegistry();

    @Test
    void everySupportedCodeHasExpectedQuestionCountAndResponseScale() {
        assertDefinition(AssessmentCode.PHQ_9, 9, 4, 0, 3);
        assertDefinition(AssessmentCode.GAD_7, 7, 4, 0, 3);
        assertDefinition(AssessmentCode.DASS_21, 21, 4, 0, 3);
        assertDefinition(AssessmentCode.PSS_10, 10, 5, 0, 4);
        assertDefinition(AssessmentCode.WHO_5, 5, 6, 0, 5);
    }

    @Test
    void phq9UsesConfiguredVietnameseFrequencyScale() {
        assertThat(registry.getRequired(AssessmentCode.PHQ_9).responseScale())
                .extracting(
                        AssessmentDefinition.ResponseScaleOption::optionText,
                        AssessmentDefinition.ResponseScaleOption::rawValue
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Không hề", 0),
                        org.assertj.core.groups.Tuple.tuple("Vài ngày", 1),
                        org.assertj.core.groups.Tuple.tuple("Hơn một nửa số ngày", 2),
                        org.assertj.core.groups.Tuple.tuple("Gần như mỗi ngày", 3)
                );
    }

    private void assertDefinition(
            AssessmentCode code,
            int questionCount,
            int optionCount,
            int minimumRawValue,
            int maximumRawValue
    ) {
        AssessmentDefinition definition = registry.getRequired(code);
        assertThat(definition.requiredQuestionCount()).isEqualTo(questionCount);
        assertThat(definition.responseScale()).hasSize(optionCount);
        assertThat(definition.responseScale().get(0).rawValue()).isEqualTo(minimumRawValue);
        assertThat(definition.responseScale().get(optionCount - 1).rawValue()).isEqualTo(maximumRawValue);
    }
}
