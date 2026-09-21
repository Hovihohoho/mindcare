package com.mindcare.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WellnessForecastRequest(
        @NotBlank String featureVersion,
        @NotNull @Size(min = 42, max = 42) List<String> featureNames,
        @NotNull @Size(min = 42, max = 42) List<Float> features
) {
    public static final String SUPPORTED_FEATURE_VERSION = "lifesnaps-features-v1";
    public static final List<String> FEATURE_NAMES = List.of(
            "hr_mean", "hr_std", "hr_min", "hr_max", "hr_hour_count",
            "resting_hr", "steps_total", "calories_total", "distance_total",
            "lightly_active_minutes", "moderately_active_minutes", "very_active_minutes",
            "active_minutes", "sedentary_minutes", "sleep_minutes",
            "sleep_duration_minutes", "sleep_awake_minutes", "sleep_efficiency",
            "hr_mean_mean_3d", "hr_mean_mean_7d", "hr_mean_delta_7d",
            "hr_std_mean_3d", "hr_std_mean_7d", "hr_std_delta_7d",
            "resting_hr_mean_3d", "resting_hr_mean_7d", "resting_hr_delta_7d",
            "steps_total_mean_3d", "steps_total_mean_7d", "steps_total_delta_7d",
            "active_minutes_mean_3d", "active_minutes_mean_7d", "active_minutes_delta_7d",
            "sedentary_minutes_mean_3d", "sedentary_minutes_mean_7d", "sedentary_minutes_delta_7d",
            "sleep_minutes_mean_3d", "sleep_minutes_mean_7d", "sleep_minutes_delta_7d",
            "sleep_efficiency_mean_3d", "sleep_efficiency_mean_7d", "sleep_efficiency_delta_7d");
    private static final int BASE_FEATURE_COUNT = 18;
    private static final double[][] BASE_BOUNDS = {
            {20, 250}, {0, 230}, {20, 250}, {20, 250}, {1, 24}, {20, 200},
            {0, 100_000}, {0, 15_000}, {0, 100_000}, {0, 1_440}, {0, 1_440},
            {0, 1_440}, {0, 1_440}, {0, 1_440}, {0, 1_440}, {0, 1_440},
            {0, 1_440}, {0, 100}
    };

    public float[] toFeatureArray() {
        if (!SUPPORTED_FEATURE_VERSION.equals(featureVersion)) {
            throw new IllegalArgumentException("Unsupported wellness feature version: " + featureVersion);
        }
        if (featureNames == null || !FEATURE_NAMES.equals(featureNames)) {
            throw new IllegalArgumentException("Wellness feature names or order do not match " + SUPPORTED_FEATURE_VERSION);
        }
        if (features == null || features.size() != FEATURE_NAMES.size()) {
            throw new IllegalArgumentException("Expected exactly 42 wellness features");
        }
        float[] result = new float[features.size()];
        boolean hasBaseFeature = false;
        for (int index = 0; index < features.size(); index++) {
            Float value = features.get(index);
            if (value == null) {
                result[index] = Float.NaN;
                continue;
            }
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Wellness features must be finite numbers or null");
            }
            if (index < BASE_FEATURE_COUNT) {
                double[] bounds = BASE_BOUNDS[index];
                if (value < bounds[0] || value > bounds[1]) {
                    throw new IllegalArgumentException("Wellness feature " + FEATURE_NAMES.get(index) + " is outside its valid range");
                }
                hasBaseFeature = true;
            }
            result[index] = value;
        }
        if (!hasBaseFeature) {
            throw new IllegalArgumentException("At least one current-day wellness feature is required");
        }
        return result;
    }
}
