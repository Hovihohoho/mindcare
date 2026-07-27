package com.mindcare.emotionservice.assessment.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AssessmentCodeJpaConverter implements AttributeConverter<AssessmentCode, String> {

    @Override
    public String convertToDatabaseColumn(AssessmentCode attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public AssessmentCode convertToEntityAttribute(String databaseValue) {
        return AssessmentCode.fromValue(databaseValue);
    }
}
