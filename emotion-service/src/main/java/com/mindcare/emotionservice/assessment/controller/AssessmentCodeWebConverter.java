package com.mindcare.emotionservice.assessment.controller;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AssessmentCodeWebConverter implements Converter<String, AssessmentCode> {

    @Override
    public AssessmentCode convert(String source) {
        return AssessmentCode.fromValue(source);
    }
}
