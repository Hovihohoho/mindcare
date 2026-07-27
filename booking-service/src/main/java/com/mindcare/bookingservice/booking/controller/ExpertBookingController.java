package com.mindcare.bookingservice.booking.controller;

import com.mindcare.bookingservice.booking.dto.BookingResponse;
import com.mindcare.bookingservice.booking.dto.CancellationDecisionRequest;
import com.mindcare.bookingservice.booking.dto.CancellationRequest;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expert/bookings")
@PreAuthorize("hasRole('EXPERT')")
@RequiredArgsConstructor
@Validated
public class ExpertBookingController {

    private final BookingService bookingService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public CursorPageResponse<BookingResponse> history(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @Size(max = 512) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return bookingService.getExpertHistory(
                currentUserProvider.getRequiredUserId(),
                status,
                cursor,
                limit);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse detail(@PathVariable UUID bookingId) {
        return bookingService.getForExpert(
                currentUserProvider.getRequiredUserId(),
                bookingId);
    }

    @PostMapping("/{bookingId}/cancellation:decide")
    public BookingResponse decideCancellation(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CancellationDecisionRequest request) {
        return bookingService.decideCancellation(
                currentUserProvider.getRequiredUserId(),
                bookingId,
                request);
    }

    @PostMapping("/{bookingId}:cancel")
    public BookingResponse cancel(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CancellationRequest request) {
        return bookingService.cancelByExpert(
                currentUserProvider.getRequiredUserId(),
                bookingId,
                request);
    }

    @PostMapping("/{bookingId}:complete")
    public BookingResponse complete(@PathVariable UUID bookingId) {
        return bookingService.complete(
                currentUserProvider.getRequiredUserId(),
                bookingId);
    }

    @PostMapping("/{bookingId}:user-no-show")
    public BookingResponse markUserNoShow(@PathVariable UUID bookingId) {
        return bookingService.markUserNoShow(
                currentUserProvider.getRequiredUserId(),
                bookingId);
    }
}
