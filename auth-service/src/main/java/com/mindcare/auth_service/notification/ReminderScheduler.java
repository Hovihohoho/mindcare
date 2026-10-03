package com.mindcare.auth_service.notification;

import com.mindcare.auth_service.dto.NotificationDtos;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.ReminderPreferenceRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private final ReminderPreferenceRepository reminders;
    private final NotificationRepository notifications;
    private final NotificationSocketHub socketHub;
    private final ExpoPushSender pushSender;
    private final ReminderCompletionClient completionClient;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendDueReminders() {
        Instant now = Instant.now();
        for (var reminder : reminders.findByEnabledTrue()) {
            if (!reminder.isDue(now)) continue;

            boolean checkIn = reminder.getReminderType().equals("DAILY_CHECK_IN");
            var status = completionClient.status(reminder.getUserId(), reminder.getTimezone());
            boolean completed = status != null
                    && (checkIn ? status.checkedInToday() : status.selfCareCompletedToday());
            if (completed) {
                reminder.markSent(now.atZone(ZoneId.of(reminder.getTimezone())).toLocalDate());
                continue;
            }

            boolean wellbeingPrompt = checkIn && status != null && status.morningCheckInRecommended();
            Notification item = notifications.save(Notification.create(
                    reminder.getUserId(), UUIDForReminder.of(reminder, now),
                    wellbeingPrompt ? "MORNING_WELLBEING_PROMPT" : reminder.getReminderType(),
                    title(checkIn, wellbeingPrompt), message(checkIn, wellbeingPrompt),
                    checkIn ? "/emotion" : "/care-plan"));
            reminder.markSent(now.atZone(ZoneId.of(reminder.getTimezone())).toLocalDate());
            socketHub.publish(item.getUserId(), NotificationDtos.Item.from(item));
            pushSender.send(item.getUserId(), item.getTitle(), item.getMessage(), item.getActionUrl(), item.getType());
        }
    }

    private String title(boolean checkIn, boolean wellbeingPrompt) {
        if (wellbeingPrompt) return "M\u1ed9t l\u1eddi h\u1ecfi th\u0103m cho h\u00f4m nay";
        return checkIn ? "M\u1ed9t ph\u00fat cho c\u1ea3m x\u00fac h\u00f4m nay"
                : "M\u1ed9t ho\u1ea1t \u0111\u1ed9ng nh\u1ecf cho ch\u00ednh m\u00ecnh";
    }

    private String message(boolean checkIn, boolean wellbeingPrompt) {
        if (wellbeingPrompt) {
            return "D\u1eef li\u1ec7u ba ng\u00e0y g\u1ea7n \u0111\u00e2y c\u00f3 v\u00e0i t\u00edn hi\u1ec7u c\u1ea7n \u0111\u01b0\u1ee3c quan t\u00e2m. "
                    + "H\u00f4m nay b\u1ea1n c\u1ea3m th\u1ea5y th\u1ebf n\u00e0o? N\u1ebfu mu\u1ed1n, h\u00e3y chia s\u1ebb v\u00e0 ghi l\u1ea1i nh\u1eadt k\u00fd c\u1ea3m x\u00fac nh\u00e9.";
        }
        return checkIn ? "B\u1ea1n \u0111ang c\u1ea3m th\u1ea5y th\u1ebf n\u00e0o? H\u00e3y ghi nh\u1eadn khi b\u1ea1n th\u1ea5y ph\u00f9 h\u1ee3p."
                : "K\u1ebf ho\u1ea1ch t\u1ef1 ch\u0103m s\u00f3c c\u1ee7a b\u1ea1n v\u1eabn \u0111ang ch\u1edd. Kh\u00f4ng sao n\u1ebfu h\u00f4m nay b\u1ea1n ch\u1ec9 l\u00e0m m\u1ed9t b\u01b0\u1edbc nh\u1ecf.";
    }

    private static final class UUIDForReminder {
        static UUID of(com.mindcare.auth_service.entity.ReminderPreference value, Instant now) {
            String key = value.getId() + ":" + now.atZone(ZoneId.of(value.getTimezone())).toLocalDate();
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }
    }
}
