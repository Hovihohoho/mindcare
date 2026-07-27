package com.mindcare.bookingservice.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @Min(1) @Max(5) short rating,
        @Size(max = 2000) String comment) {
}
