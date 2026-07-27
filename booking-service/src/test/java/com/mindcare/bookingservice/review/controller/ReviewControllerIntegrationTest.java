package com.mindcare.bookingservice.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.review.dto.ReviewResponse;
import com.mindcare.bookingservice.review.service.ReviewService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
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
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void userCanReviewCompletedBookingOnce() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(reviewService.create(eq(userId), eq(bookingId), any()))
                .thenReturn(new ReviewResponse(
                        reviewId,
                        bookingId,
                        expertId,
                        (short) 5,
                        "Rất hữu ích",
                        OffsetDateTime.parse("2026-07-25T00:00:00Z")));

        mockMvc.perform(post(
                        "/api/v1/bookings/{bookingId}/review",
                        bookingId)
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "comment": "Rất hữu ích"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/reviews/" + reviewId))
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.rating").value(5));

        verify(reviewService).create(eq(userId), eq(bookingId), any());
    }

    @Test
    void anyoneCanListPublicReviewsForExpert() throws Exception {
        UUID expertId = UUID.randomUUID();
        ReviewResponse review = new ReviewResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                expertId,
                (short) 5,
                "Hữu ích",
                OffsetDateTime.parse("2026-07-25T00:00:00Z"));
        when(reviewService.listForExpert(expertId, "cursor-1", 10))
                .thenReturn(new CursorPageResponse<>(
                        List.of(review),
                        null,
                        false));

        mockMvc.perform(get(
                        "/api/v1/experts/{expertUserId}/reviews",
                        expertId)
                        .param("cursor", "cursor-1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].expertUserId")
                        .value(expertId.toString()))
                .andExpect(jsonPath("$.items[0].rating").value(5));

        verify(reviewService).listForExpert(expertId, "cursor-1", 10);
    }
}
