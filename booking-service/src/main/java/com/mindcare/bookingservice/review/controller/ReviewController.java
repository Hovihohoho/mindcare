package com.mindcare.bookingservice.review.controller;

import com.mindcare.bookingservice.review.dto.CreateReviewRequest;
import com.mindcare.bookingservice.review.dto.ReviewResponse;
import com.mindcare.bookingservice.review.service.ReviewService;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/bookings/{bookingId}/review")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> create(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.create(
                currentUserProvider.getRequiredUserId(),
                bookingId,
                request);
        return ResponseEntity
                .created(URI.create("/api/v1/reviews/" + response.id()))
                .body(response);
    }

    @GetMapping("/experts/{expertUserId}/reviews")
    public CursorPageResponse<ReviewResponse> listForExpert(
            @PathVariable UUID expertUserId,
            @RequestParam(required = false) @Size(max = 512) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return reviewService.listForExpert(
                expertUserId,
                cursor,
                limit);
    }
}
