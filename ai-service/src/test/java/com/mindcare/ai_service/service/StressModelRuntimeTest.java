package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindcare.ai_service.config.StressModelProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StressModelRuntimeTest {

    @ParameterizedTest
    @CsvSource({
            "1, BELOW_NORMAL",
            "2, BELOW_NORMAL",
            "3, NORMAL",
            "4, ABOVE_NORMAL",
            "5, ABOVE_NORMAL"
    })
    void mapsEveryPmdataScoreAccordingToThePublishedScale(
            int stressScore,
            String expectedLevel
    ) {
        assertThat(StressModelRuntime.relativeLevel(stressScore)).isEqualTo(expectedLevel);
    }

    @Test
    void loadsExportedModelAndRunsInference() throws Exception {
        Path modelPath = Path.of("..", "ml-training", "models", "stress-classifier-v3.onnx")
                .toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isRegularFile(modelPath), "Run PMData training before this local parity test");

        StressModelProperties properties = new StressModelProperties(
                true, modelPath.toString(), "stress-classifier-v3", 25);
        StressModelRuntime runtime = new StressModelRuntime(properties);
        try {
            float[] features = new float[25];
            StressModelRuntime.Prediction prediction = runtime.predict(features);
            assertThat(prediction.stressScore()).isBetween(1, 5);
            assertThat(prediction.relativeLevel()).isIn("BELOW_NORMAL", "NORMAL", "ABOVE_NORMAL");
            assertThat(prediction.confidence()).isBetween(0.0f, 1.0f);
            assertThat(prediction.modelVersion()).isEqualTo("stress-classifier-v3");
        } finally {
            runtime.close();
        }
    }
}
