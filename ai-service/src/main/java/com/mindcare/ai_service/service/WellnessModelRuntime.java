package com.mindcare.ai_service.service;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.mindcare.ai_service.config.WellnessModelProperties;
import com.mindcare.ai_service.dto.WellnessForecastRequest;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(prefix = "ai.wellness-model", name = "enabled", havingValue = "true")
public class WellnessModelRuntime {
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private final ModelSession sleepMinutes;
    private final ModelSession steps;
    private final ModelSession restingHeartRate;
    private final WellnessModelProperties properties;

    public WellnessModelRuntime(WellnessModelProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        if (!WellnessForecastRequest.SUPPORTED_FEATURE_VERSION.equals(properties.featureVersion())) {
            throw new IllegalStateException("Unsupported configured wellness feature version: " + properties.featureVersion());
        }
        if (properties.expectedFeatures() != WellnessForecastRequest.FEATURE_NAMES.size()) {
            throw new IllegalStateException("Wellness model must use exactly 42 ordered features");
        }
        sleepMinutes = load("sleep minutes", properties.sleepMinutes(), objectMapper);
        steps = load("steps", properties.steps(), objectMapper);
        restingHeartRate = load("resting heart rate", properties.restingHeartRate(), objectMapper);
    }

    public Prediction predict(float[] features) {
        if (features == null || features.length != properties.expectedFeatures()) {
            throw new IllegalArgumentException("Expected " + properties.expectedFeatures() + " wellness features");
        }
        double predictedSleep = clamp(sleepMinutes.predict(features), 0, 1_440);
        double predictedSteps = clamp(steps.predict(features), 0, 100_000);
        double predictedRestingHeartRate = clamp(restingHeartRate.predict(features), 20, 200);
        return new Prediction(
                Math.round(predictedSleep),
                Math.round(predictedSteps),
                Math.round(predictedRestingHeartRate * 10.0) / 10.0,
                properties.version());
    }

    private ModelSession load(
            String label,
            WellnessModelProperties.ModelArtifact artifact,
            ObjectMapper objectMapper
    ) {
        if (artifact == null) {
            throw new IllegalStateException("Missing configuration for wellness " + label + " model");
        }
        Path modelPath = requiredFile(artifact.path(), "model", label);
        Path metadataPath = requiredFile(artifact.metadataPath(), "metadata", label);
        try {
            Metadata metadata = objectMapper.readValue(metadataPath.toFile(), Metadata.class);
            if (!artifact.modelVersion().equals(metadata.modelVersion())) {
                throw new IllegalStateException("Configured and metadata model versions differ for " + label);
            }
            if (!WellnessForecastRequest.FEATURE_NAMES.equals(metadata.features())) {
                throw new IllegalStateException("Metadata feature names or order are invalid for " + label);
            }
            if (!sha256(modelPath).equalsIgnoreCase(metadata.onnxSha256())) {
                throw new IllegalStateException("ONNX SHA-256 does not match metadata for " + label);
            }
            OrtSession session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
            String inputName = session.getInputNames().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Wellness " + label + " model has no input"));
            validateInputShape(session, inputName, label);
            return new ModelSession(label, session, inputName);
        } catch (IOException | OrtException exception) {
            throw new IllegalStateException("Could not load wellness " + label + " model", exception);
        }
    }

    private Path requiredFile(String configuredPath, String kind, String label) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("Missing " + kind + " path for wellness " + label);
        }
        Path path = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Wellness " + kind + " not found: " + path);
        }
        return path;
    }

    private void validateInputShape(OrtSession session, String inputName, String label) throws OrtException {
        NodeInfo node = session.getInputInfo().get(inputName);
        if (node == null || !(node.getInfo() instanceof TensorInfo tensorInfo)) {
            throw new IllegalStateException("Wellness " + label + " input is not a tensor");
        }
        long[] shape = tensorInfo.getShape();
        if (shape.length != 2 || shape[1] != properties.expectedFeatures()) {
            throw new IllegalStateException("Wellness " + label + " model input must have shape [N,42]");
        }
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            throw new IllegalStateException("Wellness model returned a non-finite prediction");
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    @PreDestroy
    void close() throws OrtException {
        sleepMinutes.close();
        steps.close();
        restingHeartRate.close();
    }

    public record Prediction(
            long sleepMinutesNextDay,
            long stepsNextDay,
            double restingHeartRateNextDay,
            String modelVersion
    ) {
    }

    private record Metadata(String modelVersion, List<String> features, String onnxSha256) {
    }

    private final class ModelSession {
        private final String label;
        private final OrtSession session;
        private final String inputName;

        private ModelSession(String label, OrtSession session, String inputName) {
            this.label = label;
            this.session = session;
            this.inputName = inputName;
        }

        private double predict(float[] features) {
            try (OnnxTensor input = OnnxTensor.createTensor(
                         environment, FloatBuffer.wrap(features), new long[]{1, features.length});
                 OrtSession.Result result = session.run(Map.of(inputName, input))) {
                Object output = result.get(0).getValue();
                if (!(output instanceof float[][] values) || values.length != 1 || values[0].length != 1) {
                    throw new IllegalStateException("Wellness " + label + " model returned an invalid output shape");
                }
                return values[0][0];
            } catch (OrtException exception) {
                throw new IllegalStateException("Wellness " + label + " inference failed", exception);
            }
        }

        private void close() throws OrtException {
            session.close();
        }
    }
}
