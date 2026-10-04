package com.mindcare.emotionservice.risk.notification;

import com.mindcare.emotionservice.risk.entity.PsychologicalAlertLogEntity;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RiskAlertNotificationClient {

    private final RestClient client;
    private final String internalSecret;

    public RiskAlertNotificationClient(
            @Value("${app.auth-service-url:http://localhost:8081}") String authServiceUrl,
            @Value("${app.internal-secret:local-internal-secret}") String internalSecret
    ) {
        this.client = RestClient.builder().baseUrl(authServiceUrl).build();
        this.internalSecret = internalSecret;
    }

    public void send(PsychologicalAlertLogEntity alert) {
        client.post()
                .uri("/api/auth/internal/notifications")
                .header("X-Internal-Secret", internalSecret)
                .body(new NotificationRequest(
                        alert.getId(),
                        alert.getUserId(),
                        "RISK_ALERT",
                        title(alert),
                        truncate(alert.getTriggerReason(), 500),
                        actionUrl(alert)
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String title(PsychologicalAlertLogEntity alert) {
        return "HEALTH_BENCHMARK".equals(alert.getAlertCategory())
                ? "Chỉ số sức khỏe cần được chú ý"
                : "Cảnh báo an toàn từ MindCare";
    }

    private String actionUrl(PsychologicalAlertLogEntity alert) {
        return "HEALTH_BENCHMARK".equals(alert.getAlertCategory()) ? "/health" : "/assessments";
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record NotificationRequest(
            UUID eventId,
            UUID userId,
            String type,
            String title,
            String message,
            String actionUrl
    ) {
    }
}
