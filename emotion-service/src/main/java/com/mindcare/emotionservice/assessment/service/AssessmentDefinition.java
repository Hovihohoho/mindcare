package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AssessmentDefinition(
        AssessmentCode code,
        int requiredQuestionCount,
        List<ResponseScaleOption> responseScale
) {
    public AssessmentDefinition {
        Objects.requireNonNull(code, "code must not be null");
        if (requiredQuestionCount < 1) {
            throw new IllegalArgumentException("requiredQuestionCount must be positive");
        }
        responseScale = List.copyOf(responseScale);
        if (responseScale.size() < 2) {
            throw new IllegalArgumentException("responseScale needs at least two options");
        }
        Set<Integer> rawValues = new HashSet<>();
        for (ResponseScaleOption option : responseScale) {
            if (option.rawValue() < 0 || !rawValues.add(option.rawValue())) {
                throw new IllegalArgumentException("responseScale raw values must be unique and non-negative");
            }
        }
    }

    public record ResponseScaleOption(String optionText, int rawValue) {
        public ResponseScaleOption {
            Objects.requireNonNull(optionText, "optionText must not be null");
            if (optionText.isBlank()) {
                throw new IllegalArgumentException("optionText must not be blank");
            }
        }
    }
}
