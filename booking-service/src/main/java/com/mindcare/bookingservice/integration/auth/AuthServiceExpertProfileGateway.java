package com.mindcare.bookingservice.integration.auth;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "booking.auth-adapter.mode", havingValue = "auth")
public class AuthServiceExpertProfileGateway
        implements ExpertProfileGateway, ExpertDirectoryGateway, ClientProfileGateway {
    private final RestClient client;
    private final String internalSecret;

    public AuthServiceExpertProfileGateway(
            @Value("${booking.auth-adapter.base-url}") String baseUrl,
            @Value("${booking.auth-adapter.internal-secret}") String internalSecret) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.internalSecret = internalSecret;
    }

    @Override
    public ExpertBookingProfile getBookingProfile(UUID expertUserId) {
        InternalProfile response = client.get()
                .uri("/api/auth/internal/experts/{id}/booking-profile", expertUserId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(InternalProfile.class);
        if (response == null) throw new IllegalStateException("Auth Service returned an empty expert profile");
        return new ExpertBookingProfile(response.expertUserId(), response.eligible(),
                response.consultationFee(), response.currency());
    }

    @Override
    public ClientProfile getClientProfile(UUID userId) {
        InternalClientProfile response = client.get()
                .uri("/api/auth/internal/users/{id}/client-profile", userId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(InternalClientProfile.class);
        if (response == null) {
            throw new IllegalStateException("Auth Service returned an empty client profile");
        }
        return new ClientProfile(
                response.userId(),
                response.fullName(),
                response.email(),
                response.createdAt());
    }

    @Override
    public ExpertDirectoryPage listBookable(ExpertDirectoryQuery query) {
        InternalExpert[] response = client.get()
                .uri("/api/auth/internal/experts")
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .body(InternalExpert[].class);
        String keyword = query.keyword() == null ? "" : query.keyword().toLowerCase(Locale.ROOT);
        List<ExpertDirectoryItem> values = Arrays.stream(response == null ? new InternalExpert[0] : response)
                .filter(item -> (item.displayName() + " "
                        + (item.headline() == null ? "" : item.headline()) + " "
                        + (item.specialties() == null ? "" : item.specialties()))
                        .toLowerCase(Locale.ROOT).contains(keyword))
                .map(item -> new ExpertDirectoryItem(item.expertUserId(), item.displayName(),
                        item.headline() == null ? "Chuyên gia tư vấn tâm lý" : item.headline(),
                        item.specialties() == null || item.specialties().isBlank()
                                ? List.of("TƯ VẤN TÂM LÝ")
                                : Arrays.stream(item.specialties().split(",")).map(String::trim).toList(),
                        item.yearsOfExperience() == null ? 0 : item.yearsOfExperience(),
                        item.consultationFee(), item.currency(), BigDecimal.ZERO, 0))
                .toList();
        int offset = decodeCursor(query.cursor());
        int end = Math.min(values.size(), offset + Math.max(1, Math.min(query.limit(), 100)));
        boolean hasMore = end < values.size();
        return new ExpertDirectoryPage(values.subList(Math.min(offset, values.size()), end),
                hasMore ? encodeCursor(end) : null, hasMore);
    }

    private int decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Math.max(0, Integer.parseInt(value.substring("offset:".length())));
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private String encodeCursor(int offset) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("offset:" + offset).getBytes(StandardCharsets.UTF_8));
    }

    private record InternalProfile(UUID expertUserId, boolean eligible,
                                   BigDecimal consultationFee, String currency) {}
    private record InternalExpert(UUID expertUserId, String displayName, String headline,
                                  String specialties, Integer yearsOfExperience,
                                  BigDecimal consultationFee, String currency) {}
    private record InternalClientProfile(
            UUID userId,
            String fullName,
            String email,
            java.time.LocalDateTime createdAt) {}
}
