package com.mindcare.emotionservice.healthmetric.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.emotionservice.healthmetric.dto.DailyHealthEvaluationResponse;
import com.mindcare.emotionservice.healthmetric.service.HealthBenchmarkService;
import com.mindcare.emotionservice.healthmetric.service.HealthMetricService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
class DailyHealthEvaluationControllerIntegrationTest {
    private static final String ENDPOINT = "/api/v1/health-metrics/daily-evaluation";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthMetricService healthMetricService;

    @MockitoBean
    private HealthBenchmarkService benchmarkService;

    @Test
    void returnsSourcedPersonalizedEvaluationForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 10);
        ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");
        var signal = new DailyHealthEvaluationResponse.Signal(
                "DAILY_SLEEP_CDC_PRIMARY_V2", "SLEEP_DURATION", "BELOW_SCIENTIFIC_BENCHMARK",
                List.of("SLEEP_BELOW_7_HOURS"), new BigDecimal("5.5"), "h", 1,
                new BigDecimal("8"), 14, new BigDecimal("-2.5"), new BigDecimal("-31.25"),
                true, true, "CDC adult sleep reference", "Supportive message",
                "CDC - About Sleep", "https://www.cdc.gov/sleep/about/index.html");
        when(benchmarkService.evaluateDay(userId, date, timezone)).thenReturn(new DailyHealthEvaluationResponse(
                date, timezone.getId(), "daily-wellness-v2.0", 28, false,
                "Not a clinical diagnosis", List.of(signal)));

        mockMvc.perform(get(ENDPOINT)
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER")
                        .param("date", date.toString())
                        .param("timezone", timezone.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationDate").value(date.toString()))
                .andExpect(jsonPath("$.ruleVersion").value("daily-wellness-v2.0"))
                .andExpect(jsonPath("$.clinicalDiagnosis").value(false))
                .andExpect(jsonPath("$.signals[0].personalBaseline").value(8))
                .andExpect(jsonPath("$.signals[0].generalThresholdTriggered").value(true))
                .andExpect(jsonPath("$.signals[0].personalDeviationTriggered").value(true))
                .andExpect(jsonPath("$.signals[0].sourceUrl").value("https://www.cdc.gov/sleep/about/index.html"));

        verify(benchmarkService).evaluateDay(userId, date, timezone);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        verifyNoInteractions(benchmarkService);
    }

    @Test
    void rejectsInvalidTimezone() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get(ENDPOINT)
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER")
                        .param("timezone", "Mars/Olympus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));

        verifyNoInteractions(benchmarkService);
    }
}
