package com.mindcare.bookingservice.schedule.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindcare.bookingservice.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpertScheduleTest {

    @Test
    void holdConfirmAndReleaseFollowLifecycle() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T10:00:00Z");
        ExpertSchedule schedule =
                ExpertSchedule.create(UUID.randomUUID(), start, start.plusHours(1));

        schedule.holdUntil(start.minusMinutes(15));
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.HELD);
        schedule.confirmBooked();
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.BOOKED);
        assertThat(schedule.getHoldExpiresAt()).isNull();
        schedule.releaseBooking();
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.AVAILABLE);
    }

    @Test
    void bookedScheduleCannotBeEdited() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T10:00:00Z");
        ExpertSchedule schedule =
                ExpertSchedule.create(UUID.randomUUID(), start, start.plusHours(1));
        schedule.holdUntil(start.minusMinutes(15));
        schedule.confirmBooked();

        assertThatThrownBy(() ->
                schedule.updateTime(start.plusHours(1), start.plusHours(2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not AVAILABLE");
    }
}
