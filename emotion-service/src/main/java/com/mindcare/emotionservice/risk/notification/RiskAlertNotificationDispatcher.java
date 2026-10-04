package com.mindcare.emotionservice.risk.notification;

import com.mindcare.emotionservice.risk.repository.PsychologicalAlertLogRepository;
import com.mindcare.emotionservice.risk.service.RiskService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RiskAlertNotificationDispatcher {

    private final PsychologicalAlertLogRepository repository;
    private final RiskAlertNotificationClient client;
    private final RiskService riskService;
    private final boolean enabled;

    public RiskAlertNotificationDispatcher(
            PsychologicalAlertLogRepository repository,
            RiskAlertNotificationClient client,
            RiskService riskService,
            @Value("${app.risk-notifications.enabled:true}") boolean enabled
    ) {
        this.repository = repository;
        this.client = client;
        this.riskService = riskService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.risk-notifications.poll-ms:5000}", initialDelay = 5000)
    public void deliverPending() {
        if (!enabled) {
            return;
        }
        repository.findByNotifiedFalseAndDeletedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 20))
                .forEach(alert -> deliver(alert.getId()));
    }

    void deliver(UUID alertId) {
        repository.findByIdAndDeletedAtIsNull(alertId).ifPresent(alert -> {
            if (Boolean.TRUE.equals(alert.getNotified())) {
                return;
            }
            try {
                client.send(alert);
                riskService.markAlertNotified(alert.getId());
            } catch (RuntimeException failure) {
                log.warn("risk_alert_notification outcome=retry alertId={} type={}",
                        alert.getId(), failure.getClass().getSimpleName());
            }
        });
    }
}
