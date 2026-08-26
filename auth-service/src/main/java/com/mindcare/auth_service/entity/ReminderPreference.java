package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reminder_preferences", schema = "auth_schema", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "reminder_type"}))
@NoArgsConstructor
public class ReminderPreference {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "reminder_type", nullable = false, length = 40, updatable = false) private String reminderType;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "local_time", nullable = false) private LocalTime localTime;
    @Column(nullable = false, length = 80) private String timezone;
    @Column(name = "last_sent_local_date") private LocalDate lastSentLocalDate;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public static ReminderPreference create(UUID userId, String type) {
        var value = new ReminderPreference(); value.id = UUID.randomUUID(); value.userId = userId;
        value.reminderType = type; value.enabled = false; value.localTime = LocalTime.of(20, 0);
        value.timezone = "Asia/Ho_Chi_Minh"; value.createdAt = OffsetDateTime.now(); value.updatedAt = value.createdAt;
        return value;
    }
    public void update(boolean enabled, LocalTime localTime, String timezone) {
        ZoneId.of(timezone); this.enabled = enabled; this.localTime = localTime; this.timezone = timezone; this.updatedAt = OffsetDateTime.now();
    }
    public boolean isDue(Instant now) {
        var local = now.atZone(ZoneId.of(timezone));
        var scheduled = local.toLocalDate().atTime(localTime).atZone(local.getZone());
        long elapsedMinutes = Duration.between(scheduled, local).toMinutes();
        return enabled && !local.toLocalDate().equals(lastSentLocalDate)
                && elapsedMinutes >= 0 && elapsedMinutes < 5;
    }
    public void markSent(LocalDate date) { this.lastSentLocalDate = date; this.updatedAt = OffsetDateTime.now(); }
}
