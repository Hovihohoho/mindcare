package com.mindcare.bookingservice.integration.auth.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpertSummaryResponse(
        UUID expertUserId,
        String displayName,
        String headline,
        List<String> specialties,
        int yearsOfExperience,
        BigDecimal consultationFee,
        String currency,
        BigDecimal averageRating,
        long reviewCount) {

    public ExpertSummaryResponse {
        specialties = List.copyOf(specialties);
    }
}
