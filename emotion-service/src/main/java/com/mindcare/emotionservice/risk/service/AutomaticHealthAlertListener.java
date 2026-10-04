package com.mindcare.emotionservice.risk.service;

import com.mindcare.emotionservice.healthmetric.event.HealthMetricsSynchronizedEvent;
import com.mindcare.emotionservice.risk.client.NotificationIntentClient;
import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AutomaticHealthAlertListener {
    private static final Logger log = LoggerFactory.getLogger(AutomaticHealthAlertListener.class);

    private final RiskService riskService;
    private final NotificationIntentClient notificationIntentClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterHealthMetricsCommitted(HealthMetricsSynchronizedEvent event) {
        try {
            for (RiskAlertResponse alert : riskService.analyzeHealthBenchmarks(event.userId())) {
                requestNotification(event, alert);
            }
        } catch (RuntimeException exception) {
            log.warn("Automatic health benchmark analysis failed after metric synchronization for userId={}",
                    event.userId(), exception);
        }
    }

    private void requestNotification(HealthMetricsSynchronizedEvent event, RiskAlertResponse alert) {
        try {
            notificationIntentClient.request(event.userId(), alert);
            riskService.markAlertNotified(alert.id());
        } catch (RuntimeException exception) {
            log.warn("Health benchmark notification intent failed for alertId={} userId={}",
                    alert.id(), event.userId(), exception);
        }
    }
}
