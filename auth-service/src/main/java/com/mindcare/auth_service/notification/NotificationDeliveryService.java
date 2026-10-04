package com.mindcare.auth_service.notification;

import com.mindcare.auth_service.dto.NotificationDtos;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {
    private final JdbcTemplate jdbc;
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final NotificationSocketHub sockets;
    private final ExpoPushSender push;
    @Value("${app.notifications.worker-enabled:true}") private boolean workerEnabled;

    @Transactional
    public void enqueue(Notification notification, boolean pushRequested) {
        notifications.flush();
        jdbc.update("INSERT INTO auth_schema.notification_delivery_jobs(notification_id, push_requested) VALUES (?, ?) ON CONFLICT DO NOTHING",
                notification.getId(), pushRequested);
    }

    @Scheduled(fixedDelayString = "${app.notifications.poll-ms:5000}", initialDelay = 30000)
    public void deliverPending() {
        if (!workerEnabled) return;
        jdbc.queryForList("""
                SELECT notification_id FROM auth_schema.notification_delivery_jobs
                WHERE attempts < 8 AND next_attempt_at <= CURRENT_TIMESTAMP
                  AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP)
                ORDER BY next_attempt_at LIMIT 20
                """, UUID.class).forEach(this::deliver);
    }

    public void deliver(UUID id) {
        var claimed = jdbc.queryForList("""
                UPDATE auth_schema.notification_delivery_jobs
                SET lease_until=CURRENT_TIMESTAMP + INTERVAL '5 minutes', attempts=attempts+1
                WHERE notification_id=? AND attempts < 8 AND next_attempt_at <= CURRENT_TIMESTAMP
                  AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP)
                RETURNING push_requested
                """, Boolean.class, id);
        if (claimed.isEmpty()) return;
        try {
            var item = notifications.findById(id);
            if (item.isPresent() && users.findById(item.get().getUserId())
                    .map(user -> Boolean.TRUE.equals(user.getIsActive())).orElse(false)) {
                Notification notification = item.get();
                if (claimed.get(0)) push.send(notification.getUserId(), notification.getTitle(), notification.getMessage(), notification.getActionUrl(), notification.getType());
                sockets.publish(notification.getUserId(), NotificationDtos.Item.from(notification));
            }
            jdbc.update("DELETE FROM auth_schema.notification_delivery_jobs WHERE notification_id=?", id);
        } catch (RuntimeException failure) {
            jdbc.update("""
                    UPDATE auth_schema.notification_delivery_jobs SET lease_until=NULL,
                      next_attempt_at=CURRENT_TIMESTAMP + LEAST(attempts * 30, 3600) * INTERVAL '1 second'
                    WHERE notification_id=?
                    """, id);
            log.warn("notification_delivery outcome=retry_or_dead_letter");
        }
    }
}
