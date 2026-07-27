package com.mindcare.bookingservice.schedule.service;

import com.mindcare.bookingservice.schedule.dto.CreateScheduleRequest;
import com.mindcare.bookingservice.schedule.dto.ScheduleResponse;
import com.mindcare.bookingservice.schedule.dto.UpdateScheduleRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    ScheduleResponse create(UUID expertUserId, CreateScheduleRequest request);

    ScheduleResponse update(UUID expertUserId, UUID scheduleId, UpdateScheduleRequest request);

    void cancel(UUID expertUserId, UUID scheduleId);

    List<ScheduleResponse> listOwn(
            UUID expertUserId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit);

    List<ScheduleResponse> listAvailable(
            UUID expertUserId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit);
}
