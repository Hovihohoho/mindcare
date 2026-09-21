package com.mindcare.ai_service.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class StressPredictionRequestTest {

    @Test
    void convertsMissingValuesToNanForModelImputation() {
        List<Float> features = new ArrayList<>(Collections.nCopies(25, null));
        features.set(0, 72.5f);

        float[] result = new StressPredictionRequest("pmdata-features-v2", features).toFeatureArray();

        assertThat(result[0]).isEqualTo(72.5f);
        assertThat(result[1]).isNaN();
    }

    @Test
    void rejectsRequestWithoutAnyCurrentDayHealthData() {
        List<Float> features = new ArrayList<>(Collections.nCopies(25, null));
        features.set(10, 70.0f);

        assertThatThrownBy(() -> new StressPredictionRequest("pmdata-features-v2", features).toFeatureArray())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current-day health feature");
    }

    @Test
    void rejectsUnknownFeatureVersion() {
        List<Float> features = new ArrayList<>(Collections.nCopies(25, null));
        features.set(0, 72.5f);

        assertThatThrownBy(() -> new StressPredictionRequest("pmdata-features-v1", features).toFeatureArray())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported stress feature version");
    }
}
