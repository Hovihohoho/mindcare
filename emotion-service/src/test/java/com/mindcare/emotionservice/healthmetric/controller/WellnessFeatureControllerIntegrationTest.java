package com.mindcare.emotionservice.healthmetric.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.emotionservice.healthmetric.dto.WellnessFeatureResponse;
import com.mindcare.emotionservice.healthmetric.service.WellnessFeatureService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WellnessFeatureControllerIntegrationTest {
    private static final String ENDPOINT = "/api/v1/health-metrics/wellness-features";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WellnessFeatureService service;

    @Test
    void returnsOwnedLifeSnapsContractForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 9);
        ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");
        var features = new ArrayList<Double>(Collections.nCopies(42, null));
        features.set(6, 4_200.0);
        when(service.build(userId, date, timezone)).thenReturn(new WellnessFeatureResponse(
                "lifesnaps-features-v1", date, timezone.getId(),
                WellnessFeatureService.FEATURE_NAMES, features, 1));

        mockMvc.perform(get(ENDPOINT)
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER")
                        .param("date", date.toString())
                        .param("timezone", timezone.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureVersion").value("lifesnaps-features-v1"))
                .andExpect(jsonPath("$.featureNames.length()").value(42))
                .andExpect(jsonPath("$.features.length()").value(42))
                .andExpect(jsonPath("$.features[6]").value(4_200.0))
                .andExpect(jsonPath("$.availableBaseFeatureCount").value(1));

        verify(service).build(userId, date, timezone);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
