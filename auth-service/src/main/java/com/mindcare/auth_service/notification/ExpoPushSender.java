package com.mindcare.auth_service.notification;
import com.mindcare.auth_service.repository.PushDeviceRepository; import java.util.Map; import lombok.RequiredArgsConstructor; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component; import org.springframework.web.client.RestClient;
@Component @RequiredArgsConstructor
public class ExpoPushSender { private final PushDeviceRepository devices; private final RestClient.Builder restClientBuilder; @Value("${app.push.enabled:false}") private boolean enabled; @Value("${app.push.expo-url:https://exp.host/--/api/v2/push/send}") private String expoUrl;
 public void send(java.util.UUID userId, String title, String body, String actionUrl) {
     if (!enabled) return;
     var client = restClientBuilder.build();
     for (var device : devices.findByUserIdAndEnabledTrue(userId)) {
         var response = client.post().uri(expoUrl).body(Map.of(
                 "to", device.getPushToken(), "title", title, "body", body, "sound", "default",
                 "data", actionUrl == null ? Map.of() : Map.of("url", actionUrl)))
                 .retrieve().body(Map.class);
         // HTTP 200 may still contain a rejected push ticket.
         if (response == null || response.containsKey("errors")
                 || !(response.get("data") instanceof Map<?, ?> ticket)
                 || !"ok".equals(ticket.get("status"))) {
             throw new IllegalStateException("Push provider did not accept delivery");
         }
     }
 }
}
