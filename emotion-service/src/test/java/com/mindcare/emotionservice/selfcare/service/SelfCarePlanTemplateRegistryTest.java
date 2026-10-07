package com.mindcare.emotionservice.selfcare.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class SelfCarePlanTemplateRegistryTest {
    private final SelfCarePlanTemplateRegistry registry = new SelfCarePlanTemplateRegistry();

    @Test
    void everyTemplateActivityHasCompleteHttpsEvidenceProvenance() {
        assertThat(registry.all()).hasSize(4);
        assertThat(registry.all()).allSatisfy(template -> {
            assertThat(template.activities()).isNotEmpty();
            assertThat(template.activities()).allSatisfy(activity -> {
                assertThat(activity.evidenceSourceTitle()).isNotBlank();
                assertThat(activity.evidenceSection()).isNotBlank();
                assertThat(activity.evidenceNote()).isNotBlank();
                URI uri = URI.create(activity.evidenceSourceUrl());
                assertThat(uri.getScheme()).isEqualTo("https");
                assertThat(uri.getHost()).isNotBlank();
            });
        });
    }
}
