package com.mindcare.bookingservice.schedule.service;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ScheduleLifecycleService {

    ScheduleSnapshot hold(UUID scheduleId, OffsetDateTime expiresAt);

    void confirm(UUID scheduleId);

    void releaseHold(UUID scheduleId);

    void releaseBooking(UUID scheduleId);

    ScheduleSnapshot get(UUID scheduleId);
}
