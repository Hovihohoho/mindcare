package com.mindcare.bookingservice.integration.auth;

public record ExpertDirectoryQuery(
        String keyword,
        String specialty,
        String cursor,
        int limit) {
}
