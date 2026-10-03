package com.mindcare.emotionservice.risk.service;

import com.mindcare.emotionservice.healthmetric.event.HealthMetricsSynchronizedEvent;
import com.mindcare.emotionservice.risk.client.NotificationIntentClient;
import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomaticHealthAlertListenerTest {
    @Mock
    private RiskService riskService;
    @Mock
    private NotificationIntentClient notificationIntentClient;

    @Test
    void analyzesAndRequestsNotificationAfterHealthSync() {
        UUID userId = UUID.randomUUID();
        RiskAlertResponse alert = new RiskAlertResponse(
                UUID.randomUUID(), "WELLNESS", "Ngủ dưới benchmark", "health-benchmark-v1",
                "SLEEP_BELOW_7_HOURS_REPEATED", null, false, null,
                "HEALTH_BENCHMARK", "SLEEP_DURATION_CDC_ADULT_V1", "1.0",
                "https://www.cdc.gov/sleep/about/index.html", "SLEEP_DURATION", null, "h",
                "SLEEP_WELLNESS_V1"
        );
        when(riskService.analyzeHealthBenchmarks(userId)).thenReturn(List.of(alert));
        AutomaticHealthAlertListener listener = new AutomaticHealthAlertListener(
                riskService, notificationIntentClient);

        listener.afterHealthMetricsCommitted(new HealthMetricsSynchronizedEvent(userId, 4));

        verify(riskService).analyzeHealthBenchmarks(userId);
        verify(notificationIntentClient).request(userId, alert);
        verify(riskService).markAlertNotified(alert.id());
    }
}
