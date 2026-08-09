package com.mindcare.bookingservice.integration.auth;

import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.review.repository.ExpertReviewRepository;
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
    private final ExpertReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public AuthServiceExpertProfileGateway(
            @Value("${booking.auth-adapter.base-url}") String baseUrl,
            @Value("${booking.auth-adapter.internal-secret}") String internalSecret,
            ExpertReviewRepository reviewRepository,
            BookingRepository bookingRepository) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.internalSecret = internalSecret;
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
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
                .map(this::directoryItem)
                .toList();
        int offset = decodeCursor(query.cursor());
        int end = Math.min(values.size(), offset + Math.max(1, Math.min(query.limit(), 100)));
        boolean hasMore = end < values.size();
        return new ExpertDirectoryPage(values.subList(Math.min(offset, values.size()), end),
                hasMore ? encodeCursor(end) : null, hasMore);
    }

    private ExpertDirectoryItem directoryItem(InternalExpert item) {
        var rating = reviewRepository.aggregateForExpert(item.expertUserId());
        BigDecimal average = rating == null || rating.getRatingAverage() == null
                ? BigDecimal.ZERO : rating.getRatingAverage();
        long reviews = rating == null ? 0 : rating.getReviewCount();
        long consultations = bookingRepository.countByExpertUserIdAndStatusAndDeletedAtIsNull(
                item.expertUserId(), BookingStatus.COMPLETED);
        List<String> specialties = item.specialties() == null || item.specialties().isBlank()
                ? List.of("TƯ VẤN TÂM LÝ")
                : Arrays.stream(item.specialties().split(","))
                        .map(String::trim).filter(value -> !value.isBlank()).toList();
        return new ExpertDirectoryItem(item.expertUserId(), item.displayName(),
                item.headline() == null ? "Chuyên gia tư vấn tâm lý" : item.headline(),
                specialties, item.yearsOfExperience() == null ? 0 : item.yearsOfExperience(),
                item.consultationFee(), item.currency(), average, reviews, consultations,
                item.avatarUrl(), item.bio(), item.workplace(), item.education());
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
                                  BigDecimal consultationFee, String currency, String avatarUrl,
                                  String bio, String workplace, String education) {}
    private record InternalClientProfile(
            UUID userId,
            String fullName,
            String email,
            java.time.LocalDateTime createdAt) {}
}
