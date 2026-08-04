package com.mindcare.bookingservice.booking.service;

import com.mindcare.bookingservice.booking.entity.Booking;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.schedule.service.ScheduleLifecycleService;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingMaintenanceServiceImpl implements BookingMaintenanceService {

    private static final int BATCH_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final ScheduleLifecycleService scheduleLifecycleService;
    private final OutboxService outboxService;
    private final Clock clock;

    @Override
    @Transactional
    public int expirePaymentHolds() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<Booking> expired = bookingRepository.findExpiredPaymentBookingsForUpdate(
                now,
                PageRequest.of(0, BATCH_SIZE));
        expired.forEach(booking -> {
            booking.expire();
            scheduleLifecycleService.releaseHold(booking.getScheduleId());
            outboxService.append(
                    "BOOKING",
                    booking.getId(),
                    "booking.expired",
                    Map.of(
                            "bookingId", booking.getId(),
                            "scheduleId", booking.getScheduleId()));
        });
        return expired.size();
    }

    @Override
    @Transactional
    public int expireCancellationRequests() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<Booking> expired = bookingRepository.findExpiredCancellationRequestsForUpdate(
                now,
                PageRequest.of(0, BATCH_SIZE));
        expired.forEach(booking -> {
            booking.timeoutCancellation(now);
            outboxService.append(
                    "BOOKING",
                    booking.getId(),
                    "booking.cancellation-rejected",
                    Map.of(
                            "bookingId", booking.getId(),
                            "userId", booking.getUserId(),
                            "reasonCode", "REQUEST_TIMEOUT"));
        });
        return expired.size();
    }

    @Override
    @Transactional
    public int createConsultationReminders() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        var schedules = scheduleLifecycleService.findBookedStartingBetween(
                now, now.plusMinutes(30), BATCH_SIZE);
        int created = 0;
        for (var schedule : schedules) {
            Booking booking = bookingRepository.findByScheduleIdAndStatusAndDeletedAtIsNull(
                    schedule.id(), BookingStatus.CONFIRMED).orElse(null);
            if (booking == null || booking.getReminderSentAt() != null) continue;
            outboxService.append("BOOKING", booking.getId(), "booking.reminder-due",
                    Map.of("bookingId", booking.getId(), "userId", booking.getUserId(),
                            "expertUserId", booking.getExpertUserId(),
                            "startAt", schedule.startAt().toString()));
            booking.markReminderSent(now);
            created++;
        }
        return created;
    }
}
