package com.mindcare.auth_service.notification;

import tools.jackson.databind.JsonNode;
import com.mindcare.auth_service.entity.ExpoPushReceipt;
import com.mindcare.auth_service.repository.ExpoPushReceiptRepository;
import com.mindcare.auth_service.repository.PushDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ExpoPushSender {
    private static final Logger log = LoggerFactory.getLogger(ExpoPushSender.class);
    private final PushDeviceRepository devices;
    private final ExpoPushReceiptRepository receipts;
    private final RestClient client;
    private final boolean enabled;
    private final String expoUrl;

    public ExpoPushSender(
            PushDeviceRepository devices,
            ExpoPushReceiptRepository receipts,
            RestClient.Builder restClientBuilder,
            @Value("${app.push.enabled:false}") boolean enabled,
            @Value("${app.push.expo-url:https://exp.host/--/api/v2/push/send}") String expoUrl
    ) {
        this.devices = devices;
        this.receipts = receipts;
        this.client = restClientBuilder.build();
        this.enabled = enabled;
        this.expoUrl = expoUrl;
    }

    public void send(UUID userId, String title, String body, String actionUrl) {
        send(userId, title, body, actionUrl, null);
    }

    public void send(UUID userId, String title, String body, String actionUrl, String notificationType) {
        if (!enabled) return;
        RuntimeException failure = null;
        for (var device : devices.findByUserIdAndEnabledTrue(userId)) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("to", device.getPushToken());
                payload.put("title", title);
                payload.put("body", lockScreenBody(notificationType, body));
                payload.put("sound", "default");
                payload.put("channelId", channelId(notificationType));
                Map<String, Object> data = new LinkedHashMap<>();
                if (actionUrl != null) data.put("url", actionUrl);
                if (notificationType != null) data.put("type", notificationType);
                payload.put("data", data);
                JsonNode response = client.post()
                        .uri(expoUrl)
                        .header("Accept", "application/json")
                        .header("Accept-Encoding", "gzip, deflate")
                        .body(payload)
                        .retrieve()
                        .body(JsonNode.class);
                handleTicket(device, response);
            } catch (RuntimeException exception) {
                log.warn("expo_push_failed deviceId={} userId={} error={}",
                        device.getId(), userId, exception.getClass().getSimpleName());
                failure = exception;
            }
        }
        if (failure != null) throw failure;
    }

    private void handleTicket(com.mindcare.auth_service.entity.PushDevice device, JsonNode response) {
        JsonNode ticket = response == null ? null : response.path("data");
        if (ticket == null || ticket.isMissingNode()) {
            log.warn("expo_push_invalid_ticket deviceId={}", device.getId());
            throw new IllegalStateException("Push provider returned an invalid ticket");
        }
        if ("ok".equals(ticket.path("status").asText())) {
            String ticketId = ticket.path("id").asText();
            log.info("expo_push_accepted deviceId={} ticketId={}",
                    device.getId(), ticketId.isBlank() ? "unknown" : ticketId);
            if (!ticketId.isBlank()) receipts.save(ExpoPushReceipt.pending(device.getId(), ticketId));
            return;
        }
        String error = ticket.path("details").path("error").asText("UNKNOWN");
        log.warn("expo_push_rejected deviceId={} error={}", device.getId(), error);
        if ("DeviceNotRegistered".equals(error)) {
            device.disable();
            devices.save(device);
            return;
        }
        throw new IllegalStateException("Push provider did not accept delivery");
    }

    private String lockScreenBody(String notificationType, String body) {
        return switch (notificationType == null ? "" : notificationType) {
            case "DAILY_CHECK_IN", "MORNING_WELLBEING_PROMPT" ->
                    "MindCare c\u00f3 m\u1ed9t l\u1eddi nh\u1eafc d\u00e0nh cho b\u1ea1n.";
            case "HEALTH_BENCHMARK", "PMDATA_STRESS_ALERT" ->
                    "M\u1edf MindCare \u0111\u1ec3 xem th\u00f4ng tin m\u1edbi.";
            default -> body;
        };
    }

    private String channelId(String notificationType) {
        return switch (notificationType == null ? "" : notificationType) {
            case "HEALTH_BENCHMARK", "PMDATA_STRESS_ALERT" -> "stress-insights";
            default -> "reminders";
        };
    }
}
