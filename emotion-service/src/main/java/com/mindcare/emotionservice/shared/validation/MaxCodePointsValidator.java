package com.mindcare.emotionservice.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxCodePointsValidator implements ConstraintValidator<MaxCodePoints, CharSequence> {

    private int maximum;

    @Override
    public void initialize(MaxCodePoints constraintAnnotation) {
        maximum = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String text = value.toString();
        return text.codePointCount(0, text.length()) <= maximum;
    }
}
