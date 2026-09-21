package com.mindcare.auth_service.notification;
import java.util.UUID; import lombok.RequiredArgsConstructor; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component; import org.springframework.web.client.RestClient;
@Component @RequiredArgsConstructor
public class ReminderCompletionClient { private final RestClient.Builder builder; @Value("${app.emotion-service-url:http://localhost:8083}") private String baseUrl; @Value("${app.internal-secret}") private String secret;
 public boolean completed(UUID userId,String type,String timezone){try{var response=builder.build().get().uri(baseUrl+"/api/v1/internal/reminder-status/{userId}?timezone={timezone}",userId,timezone).header("X-Internal-Secret",secret).retrieve().body(Status.class);return response!=null&&(type.equals("DAILY_CHECK_IN")?response.checkedInToday():response.selfCareCompletedToday());}catch(RuntimeException ignored){return false;}}
 record Status(boolean checkedInToday,boolean selfCareCompletedToday){}
}
