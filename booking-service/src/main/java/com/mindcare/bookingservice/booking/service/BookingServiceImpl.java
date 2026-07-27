package com.mindcare.bookingservice.booking.service;

import com.mindcare.bookingservice.booking.dto.BookingCheckoutResponse;
import com.mindcare.bookingservice.booking.dto.BookingResponse;
import com.mindcare.bookingservice.booking.dto.CancellationDecisionRequest;
import com.mindcare.bookingservice.booking.dto.CancellationRequest;
import com.mindcare.bookingservice.booking.dto.CreateBookingRequest;
import com.mindcare.bookingservice.booking.entity.Booking;
import com.mindcare.bookingservice.booking.entity.BookingPaymentStatus;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.mapper.BookingMapper;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.integration.auth.ExpertBookingProfile;
import com.mindcare.bookingservice.integration.auth.ExpertProfileGateway;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.payment.dto.PaymentResponse;
import com.mindcare.bookingservice.payment.entity.RefundReason;
import com.mindcare.bookingservice.payment.service.PaymentCheckoutContext;
import com.mindcare.bookingservice.payment.service.PaymentService;
import com.mindcare.bookingservice.schedule.entity.ScheduleStatus;
import com.mindcare.bookingservice.schedule.service.ScheduleLifecycleService;
import com.mindcare.bookingservice.schedule.service.ScheduleSnapshot;
import com.mindcare.bookingservice.shared.config.BookingPolicyProperties;
import com.mindcare.bookingservice.shared.config.BookingPaymentProperties;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import com.mindcare.bookingservice.shared.dto.PageCursor;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import com.mindcare.bookingservice.shared.web.CursorCodec;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingReservationService bookingReservationService;
    private final ScheduleLifecycleService scheduleLifecycleService;
    private final ExpertProfileGateway expertProfileGateway;
    private final PaymentService paymentService;
    private final OutboxService outboxService;
    private final BookingMapper bookingMapper;
    private final CursorCodec cursorCodec;
    private final BookingPolicyProperties policy;
    private final BookingPaymentProperties paymentProperties;
    private final Clock clock;

    @Override
    public BookingCheckoutResponse createCheckout(
            UUID userId,
            String idempotencyKey,
            CreateBookingRequest request) {
        validateIdempotencyKey(idempotencyKey);
        Booking existing = bookingRepository
                .findByUserIdAndIdempotencyKeyAndDeletedAtIsNull(userId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return existingCheckout(existing, userId);
        }

        ScheduleSnapshot schedule = scheduleLifecycleService.get(request.scheduleId());
        if (schedule.status() != ScheduleStatus.AVAILABLE) {
            throw new BusinessException(
                    "SCHEDULE_NOT_AVAILABLE",
                    HttpStatus.CONFLICT,
                    "The selected schedule is no longer available");
        }
        if (schedule.expertUserId().equals(userId)) {
            throw new BusinessException(
                    "SELF_BOOKING_NOT_ALLOWED",
                    HttpStatus.CONFLICT,
                    "Expert cannot book their own schedule");
        }

        ExpertBookingProfile profile =
                expertProfileGateway.getBookingProfile(schedule.expertUserId());
        validateProfile(profile, schedule.expertUserId());

        OffsetDateTime expiresAt =
                OffsetDateTime.now(clock).plus(policy.slotHoldDuration());
        Booking booking;
        try {
            booking = bookingReservationService.reserve(
                    userId,
                    new BookingReservationService.ScheduleSnapshotForBooking(
                            schedule.id(),
                            schedule.expertUserId(),
                            schedule.startAt(),
                            schedule.endAt()),
                    idempotencyKey,
                    request.note(),
                    profile.consultationFee(),
                    profile.currency(),
                    expiresAt);
        } catch (BusinessException conflict) {
            if ("SCHEDULE_NOT_AVAILABLE".equals(conflict.getCode())) {
                Booking concurrent = bookingRepository
                        .findByUserIdAndIdempotencyKeyAndDeletedAtIsNull(
                                userId,
                                idempotencyKey)
                        .orElse(null);
                if (concurrent != null) {
                    return existingCheckout(concurrent, userId);
                }
            }
            throw conflict;
        }

        try {
            if (!paymentProperties.required()) {
                Booking confirmed = bookingReservationService.confirmWithoutPayment(
                        booking.getId(),
                        OffsetDateTime.now(clock));
                return new BookingCheckoutResponse(
                        bookingMapper.toResponse(confirmed),
                        null,
                        false);
            }
            if (request.paymentMethod() == null) {
                throw new BusinessException(
                        "PAYMENT_METHOD_REQUIRED",
                        HttpStatus.BAD_REQUEST,
                        "paymentMethod is required when payment is enabled");
            }
            PaymentResponse payment = paymentService.createCheckout(
                    new PaymentCheckoutContext(
                            booking.getId(),
                            userId,
                            booking.getPrice(),
                            booking.getCurrency()),
                    request.paymentMethod(),
                    idempotencyKey,
                    expiresAt);
            return new BookingCheckoutResponse(
                    bookingMapper.toResponse(booking),
                    payment,
                    true);
        } catch (RuntimeException checkoutFailure) {
            bookingReservationService.failCheckout(booking.getId());
            throw checkoutFailure;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getForUser(UUID userId, UUID bookingId) {
        return bookingMapper.toResponse(
                bookingRepository.findByIdAndUserIdAndDeletedAtIsNull(bookingId, userId)
                        .orElseThrow(ResourceNotFoundException::new));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getForExpert(UUID expertUserId, UUID bookingId) {
        return bookingMapper.toResponse(
                bookingRepository.findByIdAndExpertUserIdAndDeletedAtIsNull(
                                bookingId,
                                expertUserId)
                        .orElseThrow(ResourceNotFoundException::new));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<BookingResponse> getUserHistory(
            UUID userId,
            BookingStatus status,
            String cursor,
            int limit) {
        PageCursor decoded = cursorCodec.decode(cursor);
        List<Booking> bookings = bookingRepository.findUserHistory(
                userId,
                status,
                decoded.createdAt(),
                decoded.id(),
                PageRequest.of(0, normalizeLimit(limit) + 1));
        return toPage(bookings, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<BookingResponse> getExpertHistory(
            UUID expertUserId,
            BookingStatus status,
            String cursor,
            int limit) {
        PageCursor decoded = cursorCodec.decode(cursor);
        List<Booking> bookings = bookingRepository.findExpertHistory(
                expertUserId,
                status,
                decoded.createdAt(),
                decoded.id(),
                PageRequest.of(0, normalizeLimit(limit) + 1));
        return toPage(bookings, limit);
    }

    @Override
    @Transactional
    public BookingResponse requestUserCancellation(
            UUID userId,
            UUID bookingId,
            CancellationRequest request) {
        Booking booking = getOwnedForUpdate(bookingId, userId, true);
        ScheduleSnapshot schedule = scheduleLifecycleService.get(booking.getScheduleId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        Duration untilStart = Duration.between(now, schedule.startAt());

        if (untilStart.compareTo(policy.cancellationClosedCutoff()) < 0) {
            throw new BusinessException(
                    "CANCELLATION_WINDOW_CLOSED",
                    HttpStatus.CONFLICT,
                    "Booking can no longer be canceled");
        }

        if (untilStart.compareTo(policy.automaticRefundCutoff()) >= 0) {
            booking.cancelImmediately(
                    com.mindcare.bookingservice.booking.entity.CancellationActor.USER,
                    request.reason(),
                    now);
            scheduleLifecycleService.releaseBooking(booking.getScheduleId());
            requestRefundIfPaid(
                    booking,
                    RefundReason.USER_CANCELED_IN_POLICY);
            appendCanceledEvent(booking);
        } else {
            OffsetDateTime responseTimeout =
                    now.plus(policy.cancellationDecisionTimeout());
            OffsetDateTime cutoffDeadline =
                    schedule.startAt().minus(policy.cancellationClosedCutoff());
            OffsetDateTime deadline = responseTimeout.isBefore(cutoffDeadline)
                    ? responseTimeout
                    : cutoffDeadline;
            booking.requestCancellation(request.reason(), now, deadline);
            outboxService.append(
                    "BOOKING",
                    booking.getId(),
                    "booking.cancellation-requested",
                    Map.of(
                            "bookingId", booking.getId(),
                            "expertUserId", booking.getExpertUserId(),
                            "decisionDeadline", deadline.toString()));
        }
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse decideCancellation(
            UUID expertUserId,
            UUID bookingId,
            CancellationDecisionRequest request) {
        Booking booking = getOwnedForUpdate(bookingId, expertUserId, false);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (booking.getCancellationReviewDeadline() == null
                || !now.isBefore(booking.getCancellationReviewDeadline())) {
            booking.timeoutCancellation(now);
            return bookingMapper.toResponse(booking);
        }

        if (request.approved()) {
            booking.approveCancellation(request.reason(), now);
            scheduleLifecycleService.releaseBooking(booking.getScheduleId());
            requestRefundIfPaid(
                    booking,
                    RefundReason.USER_CANCELED_IN_POLICY);
            appendCanceledEvent(booking);
        } else {
            booking.rejectCancellation(request.reason(), now);
            outboxService.append(
                    "BOOKING",
                    booking.getId(),
                    "booking.cancellation-rejected",
                    Map.of(
                            "bookingId", booking.getId(),
                            "userId", booking.getUserId()));
        }
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelByExpert(
            UUID expertUserId,
            UUID bookingId,
            CancellationRequest request) {
        Booking booking = getOwnedForUpdate(bookingId, expertUserId, false);
        booking.cancelByExpert(request.reason(), OffsetDateTime.now(clock));
        scheduleLifecycleService.releaseBooking(booking.getScheduleId());
        requestRefundIfPaid(booking, RefundReason.EXPERT_CANCELED);
        appendCanceledEvent(booking);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse complete(UUID expertUserId, UUID bookingId) {
        Booking booking = getOwnedForUpdate(bookingId, expertUserId, false);
        ScheduleSnapshot schedule = scheduleLifecycleService.get(booking.getScheduleId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean paymentSatisfied = !paymentProperties.required()
                || booking.getPaymentStatus() == BookingPaymentStatus.PAID;
        if (now.isBefore(schedule.endAt()) || !paymentSatisfied) {
            throw new BusinessException(
                    "BOOKING_NOT_COMPLETABLE",
                    HttpStatus.CONFLICT,
                    "Booking cannot be completed yet");
        }
        booking.complete(now);
        outboxService.append(
                "BOOKING",
                booking.getId(),
                "booking.completed",
                Map.of(
                        "bookingId", booking.getId(),
                        "userId", booking.getUserId(),
                        "expertUserId", booking.getExpertUserId()));
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse markUserNoShow(UUID expertUserId, UUID bookingId) {
        Booking booking = getOwnedForUpdate(bookingId, expertUserId, false);
        ScheduleSnapshot schedule = scheduleLifecycleService.get(booking.getScheduleId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (now.isBefore(schedule.startAt().plus(policy.noShowGracePeriod()))) {
            throw new BusinessException(
                    "NO_SHOW_GRACE_PERIOD_ACTIVE",
                    HttpStatus.CONFLICT,
                    "User no-show cannot be recorded yet");
        }
        booking.markUserNoShow(now);
        return bookingMapper.toResponse(booking);
    }

    private Booking getOwnedForUpdate(UUID bookingId, UUID actorId, boolean userActor) {
        Booking booking = bookingRepository.findActiveByIdForUpdate(bookingId)
                .orElseThrow(ResourceNotFoundException::new);
        UUID owner = userActor ? booking.getUserId() : booking.getExpertUserId();
        if (!owner.equals(actorId)) {
            throw new ResourceNotFoundException();
        }
        return booking;
    }

    private void appendCanceledEvent(Booking booking) {
        outboxService.append(
                "BOOKING",
                booking.getId(),
                "booking.canceled",
                Map.of(
                        "bookingId", booking.getId(),
                        "userId", booking.getUserId(),
                        "expertUserId", booking.getExpertUserId(),
                        "actor", booking.getCanceledBy().name()));
    }

    private CursorPageResponse<BookingResponse> toPage(List<Booking> source, int requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        boolean hasMore = source.size() > limit;
        List<Booking> page = hasMore ? source.subList(0, limit) : source;
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            Booking last = page.get(page.size() - 1);
            nextCursor = cursorCodec.encode(last.getCreatedAt(), last.getId());
        }
        return new CursorPageResponse<>(
                page.stream().map(bookingMapper::toResponse).toList(),
                nextCursor,
                hasMore);
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private void validateIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 255) {
            throw new BusinessException(
                    "INVALID_IDEMPOTENCY_KEY",
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key is required and must not exceed 255 characters");
        }
    }

    private void validateProfile(ExpertBookingProfile profile, UUID expectedExpertId) {
        if (!profile.expertUserId().equals(expectedExpertId)
                || !profile.eligible()
                || profile.consultationFee() == null
                || profile.consultationFee().signum() < 0
                || profile.currency() == null
                || !profile.currency().matches("[A-Z]{3}")) {
            throw new BusinessException(
                    "EXPERT_NOT_BOOKABLE",
                    HttpStatus.CONFLICT,
                    "Expert is not available for booking");
        }
    }

    private BookingCheckoutResponse existingCheckout(Booking booking, UUID userId) {
        if (!paymentProperties.required()) {
            return new BookingCheckoutResponse(
                    bookingMapper.toResponse(booking),
                    null,
                    false);
        }
        PaymentResponse payment = paymentService.findByBooking(booking.getId(), userId)
                .orElseThrow(() -> new BusinessException(
                        "CHECKOUT_INITIALIZATION_FAILED",
                        HttpStatus.CONFLICT,
                        "The original checkout could not be initialized"));
        return new BookingCheckoutResponse(
                bookingMapper.toResponse(booking),
                payment,
                true);
    }

    private void requestRefundIfPaid(
            Booking booking,
            RefundReason reason) {
        if (booking.getPaymentStatus() == BookingPaymentStatus.PAID) {
            paymentService.requestFullRefund(booking.getId(), reason);
        }
    }
}
