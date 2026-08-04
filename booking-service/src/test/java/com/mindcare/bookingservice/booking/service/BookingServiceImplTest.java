package com.mindcare.bookingservice.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.bookingservice.booking.dto.CancellationRequest;
import com.mindcare.bookingservice.booking.dto.CreateBookingRequest;
import com.mindcare.bookingservice.booking.entity.Booking;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.mapper.BookingMapper;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.integration.auth.ExpertBookingProfile;
import com.mindcare.bookingservice.integration.auth.ExpertProfileGateway;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.payment.entity.RefundReason;
import com.mindcare.bookingservice.payment.service.PaymentService;
import com.mindcare.bookingservice.schedule.entity.ScheduleStatus;
import com.mindcare.bookingservice.schedule.service.ScheduleLifecycleService;
import com.mindcare.bookingservice.schedule.service.ScheduleSnapshot;
import com.mindcare.bookingservice.shared.config.BookingPolicyProperties;
import com.mindcare.bookingservice.shared.config.BookingPaymentProperties;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.web.CursorCodec;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse("2026-08-01T08:00:00Z");

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingReservationService reservationService;
    @Mock
    private ScheduleLifecycleService scheduleLifecycleService;
    @Mock
    private ExpertProfileGateway expertProfileGateway;
    @Mock
    private PaymentService paymentService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private BookingMapper bookingMapper;

    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = createService(true);
    }

    @Test
    void createCheckoutConfirmsBookingWithoutCallingPaymentWhenPaymentIsDisabled() {
        service = createService(false);
        UUID userId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ScheduleSnapshot schedule = new ScheduleSnapshot(
                scheduleId,
                expertId,
                NOW.plusDays(1),
                NOW.plusDays(1).plusHours(1),
                ScheduleStatus.AVAILABLE,
                null);
        Booking reserved = Booking.createPaymentPending(
                userId,
                expertId,
                scheduleId,
                "idem-no-payment",
                "test booking",
                BigDecimal.valueOf(300000),
                "VND",
                NOW.plusMinutes(15));
        ReflectionTestUtils.setField(reserved, "id", bookingId);

        when(bookingRepository.findByUserIdAndIdempotencyKeyAndDeletedAtIsNull(
                        userId,
                        "idem-no-payment"))
                .thenReturn(Optional.empty());
        when(scheduleLifecycleService.get(scheduleId)).thenReturn(schedule);
        when(expertProfileGateway.getBookingProfile(expertId))
                .thenReturn(new ExpertBookingProfile(
                        expertId,
                        true,
                        BigDecimal.valueOf(300000),
                        "VND"));
        when(reservationService.reserve(
                        userId,
                        new BookingReservationService.ScheduleSnapshotForBooking(
                                scheduleId,
                                expertId,
                                schedule.startAt(),
                                schedule.endAt()),
                        "idem-no-payment",
                        "test booking",
                        BigDecimal.valueOf(300000),
                        "VND",
                        NOW.plusMinutes(15)))
                .thenReturn(reserved);
        when(reservationService.confirmWithoutPayment(bookingId, NOW))
                .thenAnswer(invocation -> {
                    reserved.confirmWithoutPayment(NOW);
                    return reserved;
                });

        var response = service.createCheckout(
                userId,
                "idem-no-payment",
                new CreateBookingRequest(scheduleId, "test booking", null));

        assertThat(response.paymentRequired()).isFalse();
        assertThat(response.payment()).isNull();
        assertThat(reserved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(paymentService, never()).createCheckout(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(reservationService, never()).failCheckout(bookingId);
    }

    @Test
    void userHistoryWithoutCursorUsesQueryWithoutNullableCursorParameters() {
        UUID userId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 21);
        when(bookingRepository.findUserHistory(userId, null, pageRequest))
                .thenReturn(List.of());

        service.getUserHistory(userId, null, null, 20);

        verify(bookingRepository).findUserHistory(userId, null, pageRequest);
        verify(bookingRepository, never()).findUserHistoryAfter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void userHistoryWithCursorUsesKeysetQuery() {
        UUID userId = UUID.randomUUID();
        UUID cursorId = UUID.randomUUID();
        OffsetDateTime cursorCreatedAt = NOW.minusDays(1);
        String cursor = new CursorCodec().encode(cursorCreatedAt, cursorId);
        PageRequest pageRequest = PageRequest.of(0, 21);
        when(bookingRepository.findUserHistoryAfter(
                        userId,
                        null,
                        cursorCreatedAt,
                        cursorId,
                        pageRequest))
                .thenReturn(List.of());

        service.getUserHistory(userId, null, cursor, 20);

        verify(bookingRepository).findUserHistoryAfter(
                userId,
                null,
                cursorCreatedAt,
                cursorId,
                pageRequest);
        verify(bookingRepository, never()).findUserHistory(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private BookingServiceImpl createService(boolean paymentRequired) {
        BookingPolicyProperties policy = new BookingPolicyProperties(
                Duration.ofMinutes(15),
                Duration.ofHours(24),
                Duration.ofHours(2),
                Duration.ofHours(1),
                Duration.ofHours(24),
                Duration.ofHours(24),
                Duration.ofMinutes(15),
                Duration.ofHours(24));
        Clock clock = Clock.fixed(Instant.parse("2026-08-01T08:00:00Z"), ZoneOffset.UTC);
        service = new BookingServiceImpl(
                bookingRepository,
                reservationService,
                scheduleLifecycleService,
                expertProfileGateway,
                paymentService,
                outboxService,
                bookingMapper,
                new CursorCodec(),
                policy,
                new BookingPaymentProperties(paymentRequired),
                clock);
        return service;
    }

    @Test
    void cancellationAtTwentyFourHoursIsImmediateWithFullRefund() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = confirmedBooking(bookingId, userId);
        stubOwnedBookingAndSchedule(bookingId, booking, NOW.plusHours(24));

        service.requestUserCancellation(
                userId,
                bookingId,
                new CancellationRequest("cannot attend"));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        verify(scheduleLifecycleService).releaseBooking(booking.getScheduleId());
        verify(paymentService).requestFullRefund(
                bookingId,
                RefundReason.USER_CANCELED_IN_POLICY);
    }

    @Test
    void cancellationBetweenTwoAndTwentyFourHoursWaitsForExpert() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = confirmedBooking(bookingId, userId);
        stubOwnedBookingAndSchedule(bookingId, booking, NOW.plusHours(10));

        service.requestUserCancellation(
                userId,
                bookingId,
                new CancellationRequest("cannot attend"));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLATION_PENDING);
        assertThat(booking.getCancellationReviewDeadline()).isEqualTo(NOW.plusHours(1));
        verify(paymentService, never()).requestFullRefund(
                bookingId,
                RefundReason.USER_CANCELED_IN_POLICY);
    }

    @Test
    void cancellationBelowTwoHoursIsRejected() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = confirmedBooking(bookingId, userId);
        stubOwnedBookingAndSchedule(bookingId, booking, NOW.plusMinutes(119));

        assertThatThrownBy(() -> service.requestUserCancellation(
                userId,
                bookingId,
                new CancellationRequest("cannot attend")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("CANCELLATION_WINDOW_CLOSED"));
    }

    private Booking confirmedBooking(UUID bookingId, UUID userId) {
        Booking booking = Booking.createPaymentPending(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "idem-1",
                "note",
                BigDecimal.valueOf(300000),
                "VND",
                NOW.plusMinutes(15));
        ReflectionTestUtils.setField(booking, "id", bookingId);
        booking.confirmPayment(NOW.minusMinutes(1));
        return booking;
    }

    private void stubOwnedBookingAndSchedule(
            UUID bookingId,
            Booking booking,
            OffsetDateTime startAt) {
        when(bookingRepository.findActiveByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(scheduleLifecycleService.get(booking.getScheduleId()))
                .thenReturn(new ScheduleSnapshot(
                        booking.getScheduleId(),
                        booking.getExpertUserId(),
                        startAt,
                        startAt.plusHours(1),
                        ScheduleStatus.BOOKED,
                        null));
    }
}
