package com.mindcare.ai_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.ai_service.dto.WellnessForecastRequest;
import com.mindcare.ai_service.controller.WellnessForecastController;
import com.mindcare.ai_service.security.SecurityConfig;
import com.mindcare.ai_service.service.WellnessModelRuntime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(WellnessForecastController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "ai.wellness-model.enabled=true")
class WellnessForecastControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WellnessModelRuntime runtime;

    @Test
    void forecastsAllThreeIndicatorsForUser() throws Exception {
        var features = new ArrayList<Float>(Collections.nCopies(42, null));
        features.set(6, 7_000f);
        var request = new WellnessForecastRequest(
                "lifesnaps-features-v1", WellnessForecastRequest.FEATURE_NAMES, features);
        when(runtime.predict(any(float[].class))).thenReturn(
                new WellnessModelRuntime.Prediction(410, 7_200, 68.4, "lifesnaps-v1"));

        mockMvc.perform(post("/api/ai/wellness-forecasts")
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sleepMinutesNextDay").value(410))
                .andExpect(jsonPath("$.data.stepsNextDay").value(7_200))
                .andExpect(jsonPath("$.data.restingHeartRateNextDay").value(68.4))
                .andExpect(jsonPath("$.data.clinicalDiagnosis").value(false));

        verify(runtime).predict(any(float[].class));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/ai/wellness-forecasts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(runtime);
    }
}
