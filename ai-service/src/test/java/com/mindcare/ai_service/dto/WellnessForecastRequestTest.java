package com.mindcare.ai_service.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class WellnessForecastRequestTest {

    @Test
    void convertsJsonNullToNanForOnnxImputation() {
        List<Float> features = missingFeatures();
        features.set(6, 5_000f);

        float[] result = request(features, WellnessForecastRequest.FEATURE_NAMES).toFeatureArray();

        assertThat(result[0]).isNaN();
        assertThat(result[6]).isEqualTo(5_000f);
    }

    @Test
    void rejectsWrongFeatureOrderEvenWhenVectorHasFortyTwoValues() {
        List<Float> features = missingFeatures();
        features.set(6, 5_000f);
        List<String> names = new ArrayList<>(WellnessForecastRequest.FEATURE_NAMES);
        Collections.swap(names, 0, 1);

        assertThatThrownBy(() -> request(features, names).toFeatureArray())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order");
    }

    @Test
    void rejectsUnknownVersionAndOutOfRangeCurrentValue() {
        List<Float> features = missingFeatures();
        features.set(6, 100_001f);

        assertThatThrownBy(() -> request(features, WellnessForecastRequest.FEATURE_NAMES).toFeatureArray())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("steps_total");
        assertThatThrownBy(() -> new WellnessForecastRequest(
                "lifesnaps-features-v0", WellnessForecastRequest.FEATURE_NAMES, features).toFeatureArray())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported wellness feature version");
    }

    private WellnessForecastRequest request(List<Float> features, List<String> names) {
        return new WellnessForecastRequest(WellnessForecastRequest.SUPPORTED_FEATURE_VERSION, names, features);
    }

    private List<Float> missingFeatures() {
        return new ArrayList<>(Collections.nCopies(42, null));
    }
}
