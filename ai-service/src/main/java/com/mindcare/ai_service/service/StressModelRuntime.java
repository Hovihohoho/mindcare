package com.mindcare.ai_service.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.mindcare.ai_service.config.StressModelProperties;
import jakarta.annotation.PreDestroy;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ai.stress-model", name = "enabled", havingValue = "true")
public class StressModelRuntime {
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final StressModelProperties properties;

    public StressModelRuntime(StressModelProperties properties) throws OrtException {
        this.properties = properties;
        Path modelPath = Path.of(properties.path()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(modelPath)) {
            throw new IllegalStateException("Stress model not found: " + modelPath);
        }
        environment = OrtEnvironment.getEnvironment();
        session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
        inputName = session.getInputNames().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Stress model has no input tensor"));
    }

    public Prediction predict(float[] features) {
        if (features == null || features.length != properties.expectedFeatures()) {
            throw new IllegalArgumentException("Expected " + properties.expectedFeatures() + " stress features");
        }
        try (OnnxTensor input = OnnxTensor.createTensor(
                     environment, FloatBuffer.wrap(features), new long[]{1, features.length});
             OrtSession.Result result = session.run(Map.of(inputName, input))) {
            long[] labels = (long[]) result.get(0).getValue();
            float[][] probabilities = (float[][]) result.get(1).getValue();
            int stressScore = Math.toIntExact(labels[0]);
            if (stressScore < 1 || stressScore > 5 || probabilities[0].length != 5) {
                throw new IllegalStateException("Stress model returned an invalid PMData score");
            }
            return new Prediction(
                    stressScore,
                    relativeLevel(stressScore),
                    probabilities[0][stressScore - 1],
                    properties.version());
        } catch (OrtException exception) {
            throw new IllegalStateException("Stress model inference failed", exception);
        }
    }

    static String relativeLevel(int stressScore) {
        if (stressScore < 3) {
            return "BELOW_NORMAL";
        }
        if (stressScore == 3) {
            return "NORMAL";
        }
        return "ABOVE_NORMAL";
    }

    @PreDestroy
    void close() throws OrtException {
        session.close();
    }

    public record Prediction(
            int stressScore,
            String relativeLevel,
            float confidence,
            String modelVersion
    ) {}
}
