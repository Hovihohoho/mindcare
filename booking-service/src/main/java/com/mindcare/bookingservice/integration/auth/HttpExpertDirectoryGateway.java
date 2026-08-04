package com.mindcare.bookingservice.integration.auth;

import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.review.repository.ExpertReviewRepository;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "booking.auth-adapter.mode", havingValue = "http")
public class HttpExpertDirectoryGateway implements ExpertDirectoryGateway, ExpertProfileGateway {
    private final RestClient client;
    private final ExpertReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public HttpExpertDirectoryGateway(@Value("${booking.auth-adapter.base-url:http://localhost:8081}") String baseUrl,
                                      ExpertReviewRepository reviewRepository,
                                      BookingRepository bookingRepository) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public ExpertDirectoryPage listBookable(ExpertDirectoryQuery query) {
        List<AuthExpert> all = Optional.ofNullable(client.get().uri("/api/auth/public/experts?limit=100")
                .retrieve().body(new ParameterizedTypeReference<ApiEnvelope<List<AuthExpert>>>() {}))
                .map(ApiEnvelope::data).orElse(List.of());
        List<AuthExpert> filtered = all.stream()
                .filter(item -> query.keyword() == null || searchable(item).contains(query.keyword().toLowerCase(Locale.ROOT)))
                .filter(item -> query.specialty() == null || specialties(item).stream().anyMatch(value -> value.equalsIgnoreCase(query.specialty())))
                .toList();
        int offset = decode(query.cursor());
        int end = Math.min(offset + query.limit(), filtered.size());
        List<ExpertDirectoryItem> items = filtered.subList(Math.min(offset, filtered.size()), end).stream().map(this::item).toList();
        return new ExpertDirectoryPage(items, end < filtered.size() ? encode(end) : null, end < filtered.size());
    }

    @Override
    public ExpertBookingProfile getBookingProfile(UUID expertUserId) {
        AuthExpert item;
        try {
            item = Objects.requireNonNull(client.get().uri("/api/auth/public/experts/{id}", expertUserId)
                    .retrieve().body(new ParameterizedTypeReference<ApiEnvelope<AuthExpert>>() {})).data();
        } catch (Exception exception) {
            throw new ResourceNotFoundException();
        }
        return new ExpertBookingProfile(item.id(), true,
                Optional.ofNullable(item.consultationFee()).orElse(BigDecimal.ZERO), "VND");
    }

    private ExpertDirectoryItem item(AuthExpert expert) {
        var rating = reviewRepository.aggregateForExpert(expert.id());
        BigDecimal average = rating == null || rating.getRatingAverage() == null
                ? BigDecimal.ZERO : rating.getRatingAverage();
        long reviews = rating == null ? 0 : rating.getReviewCount();
        long consultations = bookingRepository.countByExpertUserIdAndStatusAndDeletedAtIsNull(
                expert.id(), BookingStatus.COMPLETED);
        return new ExpertDirectoryItem(expert.id(), expert.fullName(),
                Optional.ofNullable(expert.headline()).orElse("Chuyên gia tâm lý"),
                specialties(expert), Optional.ofNullable(expert.yearsOfExperience()).orElse(0),
                Optional.ofNullable(expert.consultationFee()).orElse(BigDecimal.ZERO),
                "VND", average, reviews, consultations, expert.avatarUrl(), expert.bio(),
                expert.workplace(), expert.education());
    }

    private List<String> specialties(AuthExpert item) {
        return item.specialties() == null ? List.of() : Arrays.stream(item.specialties().split(","))
                .map(String::trim).filter(value -> !value.isBlank()).toList();
    }
    private String searchable(AuthExpert item) {
        return (item.fullName() + " " + Optional.ofNullable(item.headline()).orElse("")).toLowerCase(Locale.ROOT);
    }
    private int decode(String cursor) {
        if (cursor == null) return 0;
        try { return Integer.parseInt(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)); }
        catch (Exception ignored) { return 0; }
    }
    private String encode(int offset) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(offset).getBytes(StandardCharsets.UTF_8));
    }

    private record ApiEnvelope<T>(boolean success, String message, T data) {}
    private record AuthExpert(UUID id, String fullName, String headline, String specialties,
                              Integer yearsOfExperience, BigDecimal consultationFee,
                              String avatarUrl, String bio, String workplace, String education) {}
}
