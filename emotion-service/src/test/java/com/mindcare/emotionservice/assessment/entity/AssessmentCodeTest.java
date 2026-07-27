package com.mindcare.emotionservice.assessment.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentCodeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AssessmentCodeJpaConverter jpaConverter = new AssessmentCodeJpaConverter();

    @Test
    void jsonUsesCanonicalHyphenatedCode() throws Exception {
        assertEquals("\"PHQ-9\"", objectMapper.writeValueAsString(AssessmentCode.PHQ_9));
        assertEquals(
                AssessmentCode.WHO_5,
                objectMapper.readValue("\"WHO-5\"", AssessmentCode.class)
        );
    }

    @Test
    void parsingAcceptsCanonicalCodeCaseInsensitivelyAndRejectsUnknownCode() {
        assertEquals(AssessmentCode.DASS_21, AssessmentCode.fromValue("dass-21"));
        assertThrows(
                IllegalArgumentException.class,
                () -> AssessmentCode.fromValue("CUSTOM-ASSESSMENT")
        );
    }

    @Test
    void jpaConverterPreservesCanonicalDatabaseValue() {
        for (AssessmentCode code : AssessmentCode.values()) {
            String databaseValue = jpaConverter.convertToDatabaseColumn(code);
            assertEquals(code.value(), databaseValue);
            assertEquals(code, jpaConverter.convertToEntityAttribute(databaseValue));
        }
    }
}
