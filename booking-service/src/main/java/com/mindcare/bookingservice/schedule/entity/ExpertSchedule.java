package com.mindcare.bookingservice.schedule.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@Entity
@Table(name = "expert_schedules", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertSchedule extends AuditableEntity {

    @Column(name = "expert_user_id", nullable = false, updatable = false)
    private UUID expertUserId;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status;

    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static ExpertSchedule create(
            UUID expertUserId,
            OffsetDateTime startAt,
            OffsetDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new BusinessException(
                    "INVALID_SCHEDULE_RANGE",
                    HttpStatus.BAD_REQUEST,
                    "Schedule start must be before end");
        }
        ExpertSchedule schedule = new ExpertSchedule();
        schedule.expertUserId = expertUserId;
        schedule.startAt = startAt;
        schedule.endAt = endAt;
        schedule.status = ScheduleStatus.AVAILABLE;
        return schedule;
    }

    public void updateTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        requireStatus(ScheduleStatus.AVAILABLE);
        if (!startAt.isBefore(endAt)) {
            throw invalidTransition("Schedule start must be before end");
        }
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void holdUntil(OffsetDateTime expiresAt) {
        requireStatus(ScheduleStatus.AVAILABLE);
        status = ScheduleStatus.HELD;
        holdExpiresAt = expiresAt;
    }

    public void confirmBooked() {
        requireStatus(ScheduleStatus.HELD);
        status = ScheduleStatus.BOOKED;
        holdExpiresAt = null;
    }

    public void releaseHold() {
        requireStatus(ScheduleStatus.HELD);
        status = ScheduleStatus.AVAILABLE;
        holdExpiresAt = null;
    }

    public void releaseBooking() {
        requireStatus(ScheduleStatus.BOOKED);
        status = ScheduleStatus.AVAILABLE;
        holdExpiresAt = null;
    }

    public void cancel() {
        requireStatus(ScheduleStatus.AVAILABLE);
        status = ScheduleStatus.CANCELLED;
    }

    private void requireStatus(ScheduleStatus expected) {
        if (status != expected) {
            throw invalidTransition("Schedule is not " + expected);
        }
    }

    private BusinessException invalidTransition(String message) {
        return new BusinessException(
                "INVALID_SCHEDULE_TRANSITION",
                HttpStatus.CONFLICT,
                message);
    }
}
