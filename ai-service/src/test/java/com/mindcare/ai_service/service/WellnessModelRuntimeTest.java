package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindcare.ai_service.config.WellnessModelProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class WellnessModelRuntimeTest {

    @Test
    void matchesGoldenPythonOnnxPredictionsAndValidatesMetadata() throws Exception {
        Path modelDirectory = Path.of("..", "ml-training", "models").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isRegularFile(modelDirectory.resolve("lifesnaps-sleep-minutes-v1.onnx")),
                "Run LifeSnaps training before this local parity test");
        WellnessModelRuntime runtime = runtime(modelDirectory);
        float[] features = {
                70, 5, 60, 80, 12, 65, 7_000, 2_000, 5_000, 100, 30, 20, 150, 600,
                420, 480, 60, 87.5f, 69, 68, 2, 6, 5.5f, -0.5f, 64, 63, 2,
                6_500, 6_000, 1_000, 140, 130, 20, 610, 620, -20, 430, 425, -5, 88, 86, 1.5f
        };
        try {
            WellnessModelRuntime.Prediction prediction = runtime.predict(features);

            // Golden values were produced directly by Python onnxruntime from the same three artifacts.
            assertThat(prediction.sleepMinutesNextDay()).isEqualTo(405);
            assertThat(prediction.stepsNextDay()).isEqualTo(5_967);
            assertThat(prediction.restingHeartRateNextDay()).isEqualTo(62.7);
            assertThat(prediction.modelVersion()).isEqualTo("lifesnaps-v1");
        } finally {
            runtime.close();
        }
    }

    @Test
    void imputesMissingSleepAndHeartRateFeaturesInsideOnnxPipelines() throws Exception {
        Path modelDirectory = Path.of("..", "ml-training", "models").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isRegularFile(modelDirectory.resolve("lifesnaps-sleep-minutes-v1.onnx")),
                "Run LifeSnaps training before this local imputation test");
        WellnessModelRuntime runtime = runtime(modelDirectory);
        float[] features = new float[42];
        java.util.Arrays.fill(features, Float.NaN);
        features[6] = 7_000;
        features[27] = 6_500;
        features[28] = 6_000;
        features[29] = 1_000;

        try {
            WellnessModelRuntime.Prediction prediction = runtime.predict(features);

            assertThat(prediction.sleepMinutesNextDay()).isBetween(0L, 1_440L);
            assertThat(prediction.stepsNextDay()).isBetween(0L, 100_000L);
            assertThat(prediction.restingHeartRateNextDay()).isBetween(20.0, 200.0);
        } finally {
            runtime.close();
        }
    }

    private WellnessModelRuntime runtime(Path modelDirectory) throws Exception {
        WellnessModelProperties properties = new WellnessModelProperties(
                true, "lifesnaps-features-v1", 42, "lifesnaps-v1",
                artifact(modelDirectory, "lifesnaps-sleep-minutes-v1"),
                artifact(modelDirectory, "lifesnaps-steps-v1"),
                artifact(modelDirectory, "lifesnaps-resting-hr-v1"));
        return new WellnessModelRuntime(properties, new ObjectMapper());
    }

    private WellnessModelProperties.ModelArtifact artifact(Path directory, String name) {
        return new WellnessModelProperties.ModelArtifact(
                directory.resolve(name + ".onnx").toString(),
                directory.resolve(name + ".metadata.json").toString(),
                name);
    }
}
