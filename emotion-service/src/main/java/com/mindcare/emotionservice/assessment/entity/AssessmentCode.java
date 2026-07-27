package com.mindcare.emotionservice.assessment.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

public enum AssessmentCode {
    PHQ_9("PHQ-9"),
    GAD_7("GAD-7"),
    DASS_21("DASS-21"),
    PSS_10("PSS-10"),
    WHO_5("WHO-5");

    private final String value;

    AssessmentCode(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static AssessmentCode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(code -> code.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "assessment code must be PHQ-9, GAD-7, DASS-21, PSS-10 or WHO-5"
                ));
    }
}
