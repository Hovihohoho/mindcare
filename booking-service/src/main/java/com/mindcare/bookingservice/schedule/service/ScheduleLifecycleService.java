package com.mindcare.bookingservice.schedule.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

public interface ScheduleLifecycleService {

    ScheduleSnapshot hold(UUID scheduleId, OffsetDateTime expiresAt);

    void confirm(UUID scheduleId);

    void releaseHold(UUID scheduleId);

    void releaseBooking(UUID scheduleId);

    ScheduleSnapshot get(UUID scheduleId);

    List<ScheduleSnapshot> findBookedStartingBetween(OffsetDateTime from, OffsetDateTime to, int limit);
}
