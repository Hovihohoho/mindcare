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
    private final ReminderPreferenceRepository reminders; private final NotificationRepository notifications;
    private final NotificationDeliveryService delivery; private final ReminderCompletionClient completionClient;
    private final org.springframework.transaction.support.TransactionTemplate transactions;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Scheduled(cron = "0 * * * * *")
    public void sendDueReminders() {
        Instant now = Instant.now();
        for (var reminder : reminders.findByEnabledTrue()) {
            if (!reminder.isDue(now)) continue;
            boolean completed;
            try { completed = completionClient.completed(reminder.getUserId(), reminder.getReminderType(), reminder.getTimezone()); }
            catch (RuntimeException unavailable) { continue; }
            transactions.executeWithoutResult(transaction -> {
            Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtextextended(?, 0))", Boolean.class, "reminder:" + reminder.getId());
            if (!Boolean.TRUE.equals(locked)) return;
            var current = reminders.findById(reminder.getId()).orElse(null);
            if (current == null || !current.isDue(now)) return;
            current.markSent(now.atZone(ZoneId.of(current.getTimezone())).toLocalDate());
            if (completed) return;
            boolean checkIn = current.getReminderType().equals("DAILY_CHECK_IN");
            Notification item = notifications.save(Notification.create(current.getUserId(), UUIDForReminder.of(current, now), current.getReminderType(),
                    checkIn ? "Một phút cho cảm xúc hôm nay" : "Một hoạt động nhỏ cho chính mình",
                    checkIn ? "Bạn đang cảm thấy thế nào? Hãy ghi nhận khi bạn thấy phù hợp." : "Kế hoạch tự chăm sóc của bạn vẫn đang chờ. Không sao nếu hôm nay bạn chỉ làm một bước nhỏ.",
                    checkIn ? "/emotion" : "/care-plan"));
            delivery.enqueue(item, true);
            });
        }
    }
    private static final class UUIDForReminder {
        static java.util.UUID of(ReminderPreference value, Instant now) {
            String key = value.getId() + ":" + now.atZone(ZoneId.of(value.getTimezone())).toLocalDate();
            return java.util.UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
