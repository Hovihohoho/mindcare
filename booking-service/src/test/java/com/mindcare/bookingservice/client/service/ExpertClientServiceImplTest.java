package com.mindcare.bookingservice.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.bookingservice.booking.entity.Booking;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.integration.auth.ClientProfile;
import com.mindcare.bookingservice.integration.auth.ClientProfileGateway;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExpertClientServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ClientProfileGateway clientProfileGateway;

    private ExpertClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExpertClientServiceImpl(
                bookingRepository,
                clientProfileGateway);
    }

    @Test
    void getByScheduleReturnsClientOnlyForOwnedBooking() {
        UUID expertId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = booking(bookingId, userId, expertId, scheduleId);
        LocalDateTime createdAt = LocalDateTime.parse("2026-07-01T08:00:00");

        when(bookingRepository.findByScheduleIdAndExpertUserIdAndDeletedAtIsNull(
                scheduleId,
                expertId)).thenReturn(Optional.of(booking));
        when(clientProfileGateway.getClientProfile(userId))
                .thenReturn(new ClientProfile(
                        userId,
                        "Nguyễn Thị Mai",
                        "mai@example.com",
                        createdAt));

        var response = service.getBySchedule(expertId, scheduleId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("Nguyễn Thị Mai");
        assertThat(response.bookingId()).isEqualTo(bookingId);
        assertThat(response.scheduleId()).isEqualTo(scheduleId);
    }

    @Test
    void getByScheduleDoesNotQueryAuthWhenBookingIsNotOwned() {
        UUID expertId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        when(bookingRepository.findByScheduleIdAndExpertUserIdAndDeletedAtIsNull(
                scheduleId,
                expertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySchedule(expertId, scheduleId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(clientProfileGateway, never()).getClientProfile(
                org.mockito.ArgumentMatchers.any());
    }

    private Booking booking(
            UUID bookingId,
            UUID userId,
            UUID expertId,
            UUID scheduleId) {
        Booking booking = Booking.createPaymentPending(
                userId,
                expertId,
                scheduleId,
                "client-profile-test",
                null,
                BigDecimal.valueOf(300000),
                "VND",
                OffsetDateTime.parse("2026-08-01T08:15:00Z"));
        ReflectionTestUtils.setField(booking, "id", bookingId);
        booking.confirmPayment(OffsetDateTime.parse("2026-08-01T08:05:00Z"));
        return booking;
    }
}
