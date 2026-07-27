package com.mindcare.emotionservice.shared.util;

import com.mindcare.emotionservice.shared.exception.InvalidRequestException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ServiceValidator {

    public static final int MAX_PAGE_SIZE = 100;

    private ServiceValidator() {
    }

    public static void requireUserId(UUID userId) {
        if (userId == null) {
            throw new InvalidRequestException("INVALID_USER_ID", "userId must not be null");
        }
    }

    public static void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new InvalidRequestException(
                    "INVALID_PAGE_LIMIT",
                    "limit must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }

    public static void validateTimeRange(
            OffsetDateTime from,
            OffsetDateTime to,
            long maximumDays
    ) {
        if (from == null || to == null) {
            throw new InvalidRequestException("INVALID_TIME_RANGE", "from and to must not be null");
        }
        if (!from.isBefore(to)) {
            throw new InvalidRequestException("INVALID_TIME_RANGE", "from must be before to");
        }
        if (Duration.between(from, to).compareTo(Duration.ofDays(maximumDays)) > 0) {
            throw new InvalidRequestException(
                    "INVALID_TIME_RANGE",
                    "time range must not exceed " + maximumDays + " days"
            );
        }
    }

    public static String requireText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("INVALID_REQUEST", field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new InvalidRequestException(
                    "INVALID_REQUEST",
                    field + " must not exceed " + maximumLength + " characters"
            );
        }
        return normalized;
    }
}
