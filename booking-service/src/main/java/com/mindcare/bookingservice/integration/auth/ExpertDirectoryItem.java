package com.mindcare.bookingservice.integration.auth;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpertDirectoryItem(
        UUID expertUserId,
        String displayName,
        String headline,
        List<String> specialties,
        int yearsOfExperience,
        BigDecimal consultationFee,
        String currency,
        BigDecimal averageRating,
        long reviewCount,
        long consultationCount,
        String avatarUrl,
        String bio,
        String workplace,
        String education) {

    public ExpertDirectoryItem {
        specialties = List.copyOf(specialties);
    }
}
