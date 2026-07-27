package com.mindcare.bookingservice.booking.service;

import com.mindcare.bookingservice.booking.entity.Booking;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.schedule.service.ScheduleLifecycleService;
import com.mindcare.bookingservice.schedule.service.ScheduleSnapshot;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingLifecycleServiceImpl implements
        BookingReservationService,
        BookingPaymentLifecycleService,
        BookingAccessService {

    private final BookingRepository bookingRepository;
    private final ScheduleLifecycleService scheduleLifecycleService;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public Booking reserve(
            UUID userId,
            ScheduleSnapshotForBooking schedule,
            String idempotencyKey,
            String note,
            BigDecimal price,
            String currency,
            OffsetDateTime expiresAt) {
        scheduleLifecycleService.hold(schedule.scheduleId(), expiresAt);
        Booking booking = bookingRepository.save(Booking.createPaymentPending(
                userId,
                schedule.expertUserId(),
                schedule.scheduleId(),
                idempotencyKey,
                note,
                price,
                currency,
                expiresAt));
        outboxService.append(
                "BOOKING",
                booking.getId(),
                "booking.payment-pending",
                Map.of(
                        "bookingId", booking.getId(),
                        "userId", userId,
                        "expertUserId", schedule.expertUserId(),
                        "scheduleId", schedule.scheduleId(),
                        "startAt", schedule.startAt().toString()));
        return booking;
    }

    @Override
    @Transactional
    public Booking confirmWithoutPayment(
            UUID bookingId,
            OffsetDateTime confirmedAt) {
        Booking booking = getForUpdate(bookingId);
        booking.confirmWithoutPayment(confirmedAt);
        scheduleLifecycleService.confirm(booking.getScheduleId());
        outboxService.append(
                "BOOKING",
                booking.getId(),
                "booking.confirmed",
                Map.of(
                        "bookingId", booking.getId(),
                        "userId", booking.getUserId(),
                        "expertUserId", booking.getExpertUserId(),
                        "scheduleId", booking.getScheduleId(),
                        "paymentRequired", false));
        return booking;
    }

    @Override
    @Transactional
    public void failCheckout(UUID bookingId) {
        Booking booking = getForUpdate(bookingId);
        if (booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
            booking.markPaymentFailed();
            scheduleLifecycleService.releaseHold(booking.getScheduleId());
        }
    }

    @Override
    @Transactional
    public boolean confirmPaymentIfActive(UUID bookingId, OffsetDateTime paidAt) {
        Booking booking = getForUpdate(bookingId);
        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING
                || booking.getExpiresAt() == null
                || !paidAt.isBefore(booking.getExpiresAt())) {
            if (booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
                booking.expire();
                scheduleLifecycleService.releaseHold(booking.getScheduleId());
            }
            return false;
        }

        booking.confirmPayment(paidAt);
        scheduleLifecycleService.confirm(booking.getScheduleId());
        outboxService.append(
                "BOOKING",
                booking.getId(),
                "booking.confirmed",
                Map.of(
                        "bookingId", booking.getId(),
                        "userId", booking.getUserId(),
                        "expertUserId", booking.getExpertUserId(),
                        "scheduleId", booking.getScheduleId()));
        return true;
    }

    @Override
    @Transactional
    public void failPayment(UUID bookingId) {
        Booking booking = getForUpdate(bookingId);
        if (booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
            booking.markPaymentFailed();
            scheduleLifecycleService.releaseHold(booking.getScheduleId());
        }
    }

    @Override
    @Transactional
    public void expireBooking(UUID bookingId) {
        Booking booking = getForUpdate(bookingId);
        if (booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
            booking.expire();
            scheduleLifecycleService.releaseHold(booking.getScheduleId());
        }
    }

    @Override
    @Transactional
    public void markRefunded(UUID bookingId) {
        getForUpdate(bookingId).markRefunded();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingAccessSnapshot getRequired(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(ResourceNotFoundException::new);
        return accessSnapshot(booking);
    }

    @Override
    @Transactional
    public BookingAccessSnapshot getRequiredForUpdate(UUID bookingId) {
        return accessSnapshot(getForUpdate(bookingId));
    }

    private BookingAccessSnapshot accessSnapshot(Booking booking) {
        ScheduleSnapshot schedule = scheduleLifecycleService.get(booking.getScheduleId());
        return new BookingAccessSnapshot(
                booking.getId(),
                booking.getUserId(),
                booking.getExpertUserId(),
                booking.getScheduleId(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                schedule.startAt(),
                schedule.endAt());
    }

    private Booking getForUpdate(UUID bookingId) {
        return bookingRepository.findActiveByIdForUpdate(bookingId)
                .orElseThrow(ResourceNotFoundException::new);
    }
}
