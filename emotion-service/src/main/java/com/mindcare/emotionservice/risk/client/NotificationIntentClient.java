package com.mindcare.emotionservice.risk.client;

import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Component
public class NotificationIntentClient {
    private final RestClient restClient;
    private final String internalSecret;

    public NotificationIntentClient(
            @Value("${app.auth-service-url:http://localhost:8081}") String authServiceUrl,
            @Value("${app.internal-secret}") String internalSecret
    ) {
        this.restClient = RestClient.builder().baseUrl(authServiceUrl).build();
        this.internalSecret = internalSecret;
    }

    public void request(UUID userId, RiskAlertResponse alert) {
        restClient.post()
                .uri("/api/auth/internal/notifications")
                .header("X-Internal-Secret", internalSecret)
                .body(new NotificationRequest(
                        alert.id(),
                        userId,
                        "HEALTH_BENCHMARK",
                        title(alert.metricType()),
                        alert.triggerReason(),
                        "/health-connect"
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public void requestStressPrediction(UUID userId, UUID predictionId, int stressScore, String alertLevel) {
        UUID notificationEventId = UUID.nameUUIDFromBytes(
                (predictionId + ":score:" + stressScore).getBytes(StandardCharsets.UTF_8));
        String title = stressScore == 5 ? "Mức stress PMData rất cao" : "Mức stress PMData cao";
        String message = "Điểm stress ước lượng là " + stressScore
                + "/5 (" + alertLevel + "). Kết quả dùng để hỗ trợ tự theo dõi, không phải chẩn đoán y khoa.";
        restClient.post()
                .uri("/api/auth/internal/notifications")
                .header("X-Internal-Secret", internalSecret)
                .body(new NotificationRequest(
                        notificationEventId, userId, "PMDATA_STRESS_ALERT", title, message, "/health-connect"))
                .retrieve()
                .toBodilessEntity();
    }

    private String title(String metricType) {
        return switch (metricType == null ? "" : metricType) {
            case "SLEEP_DURATION" -> "Giấc ngủ cần được lưu ý";
            case "HEART_RATE" -> "Nhịp tim nghỉ cần được kiểm tra";
            case "STEP_COUNT" -> "Mức vận động gần đây thấp";
            default -> "Chỉ số sức khỏe cần được lưu ý";
        };
    }

    record NotificationRequest(
            UUID eventId,
            UUID userId,
            String type,
            String title,
            String message,
            String actionUrl
    ) {
    }
}
