package com.mindcare.bookingservice.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.booking.dto.BookingCheckoutResponse;
import com.mindcare.bookingservice.booking.dto.BookingResponse;
import com.mindcare.bookingservice.booking.entity.BookingPaymentStatus;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.booking.service.BookingService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserBookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void userCanCreateConfirmedBookingWithoutPaymentInDevelopmentMode()
            throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-07-25T00:00:00Z");
        BookingResponse booking = new BookingResponse(
                bookingId,
                userId,
                expertId,
                scheduleId,
                BookingStatus.CONFIRMED,
                "Cần trao đổi về căng thẳng",
                new BigDecimal("300000.00"),
                "VND",
                BookingPaymentStatus.UNPAID,
                null,
                null,
                now,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
        when(bookingService.createCheckout(
                eq(userId),
                eq("booking-create-001"),
                any()))
                .thenReturn(new BookingCheckoutResponse(
                        booking,
                        null,
                        false));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER")
                        .header("Idempotency-Key", "booking-create-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduleId": "%s",
                                  "note": "Cần trao đổi về căng thẳng"
                                }
                                """.formatted(scheduleId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/bookings/" + bookingId))
                .andExpect(jsonPath("$.booking.id")
                        .value(bookingId.toString()))
                .andExpect(jsonPath("$.booking.status")
                        .value("CONFIRMED"))
                .andExpect(jsonPath("$.booking.paymentStatus")
                        .value("UNPAID"))
                .andExpect(jsonPath("$.payment").doesNotExist())
                .andExpect(jsonPath("$.paymentRequired").value(false));

        verify(bookingService).createCheckout(
                eq(userId),
                eq("booking-create-001"),
                any());
    }

    @Test
    void userCanGetOwnBookingHistoryWithCursorPagination()
            throws Exception {
        UUID userId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(userId);
        when(bookingService.getUserHistory(
                userId,
                BookingStatus.CONFIRMED,
                "cursor-1",
                10))
                .thenReturn(new CursorPageResponse<>(
                        List.of(booking),
                        "cursor-2",
                        true));

        mockMvc.perform(get("/api/v1/bookings")
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER")
                        .param("status", "CONFIRMED")
                        .param("cursor", "cursor-1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id")
                        .value(booking.id().toString()))
                .andExpect(jsonPath("$.nextCursor").value("cursor-2"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(bookingService).getUserHistory(
                userId,
                BookingStatus.CONFIRMED,
                "cursor-1",
                10);
    }

    @Test
    void userCanGetOwnBookingDetail() throws Exception {
        UUID userId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(userId);
        when(bookingService.getForUser(userId, booking.id()))
                .thenReturn(booking);

        mockMvc.perform(get(
                        "/api/v1/bookings/{bookingId}",
                        booking.id())
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.id().toString()))
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingService).getForUser(userId, booking.id());
    }

    @Test
    void userCanRequestBookingCancellation() throws Exception {
        UUID userId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(userId);
        when(bookingService.requestUserCancellation(
                eq(userId),
                eq(booking.id()),
                any()))
                .thenReturn(booking);

        mockMvc.perform(post(
                        "/api/v1/bookings/{bookingId}:cancel",
                        booking.id())
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Không thể tham dự"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.id().toString()));

        verify(bookingService).requestUserCancellation(
                eq(userId),
                eq(booking.id()),
                any());
    }

    private BookingResponse bookingResponse(UUID userId) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-25T00:00:00Z");
        return new BookingResponse(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                BookingStatus.CONFIRMED,
                null,
                new BigDecimal("300000.00"),
                "VND",
                BookingPaymentStatus.UNPAID,
                null,
                null,
                now,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }
}
