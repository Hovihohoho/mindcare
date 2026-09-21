package com.mindcare.emotionservice.healthmetric.dto;

import java.time.LocalDate;
import java.util.List;

public record StressFeatureResponse(
        String featureVersion,
        LocalDate featureDate,
        String timezone,
        List<String> featureNames,
        List<Double> features,
        int availableBaseFeatureCount
) {
}
