package com.mindcare.ai_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mindcare.ai_service.dto.WellnessForecastRequest;
import com.mindcare.ai_service.service.WellnessModelRuntime;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class WellnessForecastControllerTest {

    @Test
    void returnsExplicitlyNonClinicalForecast() {
        WellnessModelRuntime runtime = mock(WellnessModelRuntime.class);
        when(runtime.predict(any(float[].class))).thenReturn(
                new WellnessModelRuntime.Prediction(410, 7_200, 68.4, "lifesnaps-v1"));
        WellnessForecastController controller = new WellnessForecastController(runtime);
        var features = new ArrayList<Float>(Collections.nCopies(42, null));
        features.set(6, 7_000f);

        var response = controller.predict(new WellnessForecastRequest(
                "lifesnaps-features-v1", WellnessForecastRequest.FEATURE_NAMES, features));

        assertThat(response.success()).isTrue();
        assertThat(response.data().sleepMinutesNextDay()).isEqualTo(410);
        assertThat(response.data().stepsNextDay()).isEqualTo(7_200);
        assertThat(response.data().restingHeartRateNextDay()).isEqualTo(68.4);
        assertThat(response.data().modelVersion()).isEqualTo("lifesnaps-v1");
        assertThat(response.data().clinicalDiagnosis()).isFalse();
        assertThat(response.data().usageNotice()).contains("not a clinical diagnosis");
    }
}
