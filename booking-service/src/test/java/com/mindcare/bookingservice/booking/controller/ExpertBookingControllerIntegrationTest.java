package com.mindcare.bookingservice.booking.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.booking.dto.BookingResponse;
import com.mindcare.bookingservice.booking.entity.BookingPaymentStatus;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.booking.service.BookingService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpertBookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void expertCanGetOwnBookingHistory() throws Exception {
        UUID expertId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(expertId);
        when(bookingService.getExpertHistory(
                expertId,
                BookingStatus.CONFIRMED,
                "cursor-1",
                10))
                .thenReturn(new CursorPageResponse<>(
                        List.of(booking),
                        null,
                        false));

        mockMvc.perform(get("/api/v1/expert/bookings")
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT")
                        .param("status", "CONFIRMED")
                        .param("cursor", "cursor-1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id")
                        .value(booking.id().toString()))
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(bookingService).getExpertHistory(
                expertId,
                BookingStatus.CONFIRMED,
                "cursor-1",
                10);
    }

    @Test
    void expertCanGetOwnBookingDetail() throws Exception {
        UUID expertId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(expertId);
        when(bookingService.getForExpert(expertId, booking.id()))
                .thenReturn(booking);

        mockMvc.perform(get(
                        "/api/v1/expert/bookings/{bookingId}",
                        booking.id())
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.id().toString()))
                .andExpect(jsonPath("$.expertUserId")
                        .value(expertId.toString()));

        verify(bookingService).getForExpert(expertId, booking.id());
    }

    @Test
    void expertCanApproveCancellationRequest() throws Exception {
        UUID expertId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(expertId);
        when(bookingService.decideCancellation(
                eq(expertId),
                eq(booking.id()),
                any()))
                .thenReturn(booking);

        mockMvc.perform(post(
                        "/api/v1/expert/bookings/{bookingId}/cancellation:decide",
                        booking.id())
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true,
                                  "reason": "Đồng ý hỗ trợ người dùng"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.id().toString()));

        verify(bookingService).decideCancellation(
                eq(expertId),
                eq(booking.id()),
                any());
    }

    @Test
    void expertCanCancelOwnBooking() throws Exception {
        UUID expertId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(expertId);
        when(bookingService.cancelByExpert(
                eq(expertId),
                eq(booking.id()),
                any()))
                .thenReturn(booking);

        mockMvc.perform(post(
                        "/api/v1/expert/bookings/{bookingId}:cancel",
                        booking.id())
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Có việc khẩn cấp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.id().toString()));

        verify(bookingService).cancelByExpert(
                eq(expertId),
                eq(booking.id()),
                any());
    }

    @Test
    void expertCanCompleteEligibleBooking() throws Exception {
        UUID expertId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(expertId);
        when(bookingService.complete(expertId, booking.id()))
                .thenReturn(booking);

        mockMvc.perform(post(
                        "/api/v1/expert/bookings/{bookingId}:complete",
                        booking.id())
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.id().toString()));

        verify(bookingService).complete(expertId, booking.id());
    }

    @Test
    void expertCanMarkUserNoShowAfterGracePeriod() throws Exception {
        UUID expertId = UUID.randomUUID();
        BookingResponse booking = bookingResponse(expertId);
        when(bookingService.markUserNoShow(expertId, booking.id()))
                .thenReturn(booking);

        mockMvc.perform(post(
                        "/api/v1/expert/bookings/{bookingId}:user-no-show",
                        booking.id())
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.id().toString()));

        verify(bookingService).markUserNoShow(expertId, booking.id());
    }

    private BookingResponse bookingResponse(UUID expertId) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-25T00:00:00Z");
        return new BookingResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                expertId,
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
