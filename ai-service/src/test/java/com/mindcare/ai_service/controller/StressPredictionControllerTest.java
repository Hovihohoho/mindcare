package com.mindcare.ai_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mindcare.ai_service.dto.StressPredictionRequest;
import com.mindcare.ai_service.service.StressModelRuntime;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class StressPredictionControllerTest {

    @Test
    void returnsVersionedNonClinicalStressSignal() {
        StressModelRuntime runtime = mock(StressModelRuntime.class);
        when(runtime.predict(any(float[].class))).thenReturn(
                new StressModelRuntime.Prediction(4, "ABOVE_NORMAL", 0.73f, "stress-classifier-v3"));
        StressPredictionController controller = new StressPredictionController(runtime);
        var features = new ArrayList<Float>(Collections.nCopies(25, null));
        features.set(3, 4200.0f);

        var response = controller.predict(new StressPredictionRequest("pmdata-features-v2", features));

        assertThat(response.success()).isTrue();
        assertThat(response.data().stressScore()).isEqualTo(4);
        assertThat(response.data().relativeLevel()).isEqualTo("ABOVE_NORMAL");
        assertThat(response.data().scaleMinimum()).isEqualTo(1);
        assertThat(response.data().scaleMaximum()).isEqualTo(5);
        assertThat(response.data().confidence()).isEqualTo(0.73f);
        assertThat(response.data().modelVersion()).isEqualTo("stress-classifier-v3");
        assertThat(response.data().usageNotice()).contains("not a clinical diagnosis");
    }
}
