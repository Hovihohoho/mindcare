package com.mindcare.bookingservice.booking.controller;

import com.mindcare.bookingservice.booking.dto.BookingCheckoutResponse;
import com.mindcare.bookingservice.booking.dto.BookingResponse;
import com.mindcare.bookingservice.booking.dto.CreateBookingRequest;
import com.mindcare.bookingservice.booking.dto.CancellationRequest;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Validated
public class UserBookingController {

    private final BookingService bookingService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<BookingCheckoutResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingCheckoutResponse response = bookingService.createCheckout(
                currentUserProvider.getRequiredUserId(),
                idempotencyKey,
                request);
        return ResponseEntity
                .created(URI.create(
                        "/api/v1/bookings/" + response.booking().id()))
                .body(response);
    }

    @GetMapping
    public CursorPageResponse<BookingResponse> history(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @Size(max = 512) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return bookingService.getUserHistory(
                currentUserProvider.getRequiredUserId(),
                status,
                cursor,
                limit);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse detail(@PathVariable UUID bookingId) {
        return bookingService.getForUser(
                currentUserProvider.getRequiredUserId(),
                bookingId);
    }

    @PostMapping("/{bookingId}:cancel")
    public BookingResponse cancel(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CancellationRequest request) {
        return bookingService.requestUserCancellation(
                currentUserProvider.getRequiredUserId(),
                bookingId,
                request);
    }
}
