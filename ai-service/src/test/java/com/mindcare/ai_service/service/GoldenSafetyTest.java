package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GoldenSafetyTest {
    @Test
    void versionedVietnameseCasesHaveExpectedSafetyRouting() throws Exception {
        var root = new ObjectMapper().readTree(Files.readString(Path.of("evaluation/golden.vi.v1.json")));
        assertThat(root.path("cases").size()).isGreaterThanOrEqualTo(50);
        var detector = new CrisisRiskDetector();
        for (var item : root.path("cases")) {
            assertThat(detector.detect(item.path("question").asText()).name())
                    .as(item.path("id").asText() + ": " + item.path("question").asText())
                    .isEqualTo(item.path("expectedSafety").asText());
        }
    }
}
