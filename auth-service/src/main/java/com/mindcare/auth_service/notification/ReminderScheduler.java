package com.mindcare.auth_service.notification;

import com.mindcare.auth_service.dto.NotificationDtos;
import com.mindcare.auth_service.entity.*;
import com.mindcare.auth_service.repository.*;
import jakarta.transaction.Transactional;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class ReminderScheduler {
    private final ReminderPreferenceRepository reminders; private final NotificationRepository notifications; private final NotificationSocketHub socketHub;
    private final ExpoPushSender pushSender; private final ReminderCompletionClient completionClient;
    @Scheduled(cron = "0 * * * * *") @Transactional
    public void sendDueReminders() {
        Instant now = Instant.now();
        for (var reminder : reminders.findByEnabledTrue()) {
            if (!reminder.isDue(now)) continue;
            boolean checkIn = reminder.getReminderType().equals("DAILY_CHECK_IN");
            if (completionClient.completed(reminder.getUserId(), reminder.getReminderType(), reminder.getTimezone())) { reminder.markSent(now.atZone(ZoneId.of(reminder.getTimezone())).toLocalDate()); continue; }
            Notification item = notifications.save(Notification.create(reminder.getUserId(), UUIDForReminder.of(reminder, now), reminder.getReminderType(),
                    checkIn ? "Một phút cho cảm xúc hôm nay" : "Một hoạt động nhỏ cho chính mình",
                    checkIn ? "Bạn đang cảm thấy thế nào? Hãy ghi nhận khi bạn thấy phù hợp." : "Kế hoạch tự chăm sóc của bạn vẫn đang chờ. Không sao nếu hôm nay bạn chỉ làm một bước nhỏ.",
                    checkIn ? "/emotion" : "/care-plan"));
            reminder.markSent(now.atZone(ZoneId.of(reminder.getTimezone())).toLocalDate());
            socketHub.publish(item.getUserId(), NotificationDtos.Item.from(item));
            pushSender.send(item.getUserId(), item.getTitle(), item.getMessage(), item.getActionUrl());
        }
    }
    private static final class UUIDForReminder {
        static java.util.UUID of(ReminderPreference value, Instant now) {
            String key = value.getId() + ":" + now.atZone(ZoneId.of(value.getTimezone())).toLocalDate();
            return java.util.UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
