package com.mindcare.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StressPredictionRequest(
        @NotBlank String featureVersion,
        @NotNull @Size(min = 25, max = 25) List<Float> features
) {
    public static final String SUPPORTED_FEATURE_VERSION = "pmdata-features-v2";
    private static final int BASE_FEATURE_COUNT = 10;

    public float[] toFeatureArray() {
        if (!SUPPORTED_FEATURE_VERSION.equals(featureVersion)) {
            throw new IllegalArgumentException(
                    "Unsupported stress feature version: " + featureVersion);
        }
        if (features == null || features.size() != 25) {
            throw new IllegalArgumentException("Expected exactly 25 stress features");
        }

        float[] values = new float[features.size()];
        boolean hasBaseFeature = false;
        for (int index = 0; index < features.size(); index++) {
            Float value = features.get(index);
            if (value == null) {
                values[index] = Float.NaN;
                continue;
            }
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Stress features must be finite numbers or null");
            }
            values[index] = value;
            if (index < BASE_FEATURE_COUNT) {
                hasBaseFeature = true;
            }
        }
        if (!hasBaseFeature) {
            throw new IllegalArgumentException("At least one current-day health feature is required");
        }
        return values;
    }
}
