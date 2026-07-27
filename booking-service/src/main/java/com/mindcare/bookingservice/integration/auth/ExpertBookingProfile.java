package com.mindcare.bookingservice.integration.auth;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpertBookingProfile(
        UUID expertUserId,
        boolean eligible,
        BigDecimal consultationFee,
        String currency) {
}
