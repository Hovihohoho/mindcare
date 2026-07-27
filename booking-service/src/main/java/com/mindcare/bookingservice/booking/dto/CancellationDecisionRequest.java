package com.mindcare.bookingservice.booking.dto;

import jakarta.validation.constraints.Size;

public record CancellationDecisionRequest(
        boolean approved,
        @Size(max = 1000) String reason) {
}
