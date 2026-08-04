package com.mindcare.bookingservice.integration.auth;

import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "booking.auth-adapter.mode",
        havingValue = "local")
public class LocalExpertDirectoryGateway
        implements ExpertDirectoryGateway, ExpertProfileGateway, ClientProfileGateway {

    private static final List<ExpertDirectoryItem> EXPERTS = List.of(
            new ExpertDirectoryItem(
                    UUID.fromString("11111111-1111-4111-8111-111111111111"),
                    "Chuyên gia An",
                    "Tham vấn tâm lý học đường",
                    List.of("STRESS", "ANXIETY"),
                    8,
                    new BigDecimal("300000.00"),
                    "VND",
                    new BigDecimal("4.80"),
                    124),
            new ExpertDirectoryItem(
                    UUID.fromString("22222222-2222-4222-8222-222222222222"),
                    "Chuyên gia Bình",
                    "Tâm lý lâm sàng",
                    List.of("DEPRESSION", "ANXIETY"),
                    10,
                    new BigDecimal("400000.00"),
                    "VND",
                    new BigDecimal("4.90"),
                    98),
            new ExpertDirectoryItem(
                    UUID.fromString("33333333-3333-4333-8333-333333333333"),
                    "Chuyên gia Chi",
                    "Sức khỏe tinh thần người trẻ",
                    List.of("STRESS", "SLEEP"),
                    6,
                    new BigDecimal("250000.00"),
                    "VND",
                    new BigDecimal("4.70"),
                    76));

    @Override
    public ExpertDirectoryPage listBookable(ExpertDirectoryQuery query) {
        List<ExpertDirectoryItem> filtered = EXPERTS.stream()
                .filter(item -> matchesKeyword(item, query.keyword()))
                .filter(item -> matchesSpecialty(item, query.specialty()))
                .toList();
        int offset = decodeCursor(query.cursor());
        if (offset > filtered.size()) {
            throw invalidCursor();
        }
        int end = Math.min(offset + query.limit(), filtered.size());
        boolean hasMore = end < filtered.size();
        String nextCursor = hasMore ? encodeCursor(end) : null;
        return new ExpertDirectoryPage(
                filtered.subList(offset, end),
                nextCursor,
                hasMore);
    }

    @Override
    public ExpertBookingProfile getBookingProfile(UUID expertUserId) {
        return EXPERTS.stream()
                .filter(item -> item.expertUserId().equals(expertUserId))
                .findFirst()
                .map(item -> new ExpertBookingProfile(
                        item.expertUserId(),
                        true,
                        item.consultationFee(),
                        item.currency()))
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Override
    public ClientProfile getClientProfile(UUID userId) {
        return new ClientProfile(
                userId,
                "Khách hàng " + userId.toString().substring(0, 8),
                null,
                null);
    }

    private boolean matchesKeyword(ExpertDirectoryItem item, String keyword) {
        if (keyword == null) {
            return true;
        }
        String expected = keyword.toLowerCase(Locale.ROOT);
        return item.displayName().toLowerCase(Locale.ROOT).contains(expected)
                || item.headline().toLowerCase(Locale.ROOT).contains(expected);
    }

    private boolean matchesSpecialty(ExpertDirectoryItem item, String specialty) {
        return specialty == null
                || item.specialties().stream()
                        .anyMatch(value -> value.equalsIgnoreCase(specialty));
    }

    private int decodeCursor(String cursor) {
        if (cursor == null) {
            return 0;
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8);
            if (!decoded.startsWith("offset:")) {
                throw invalidCursor();
            }
            int offset = Integer.parseInt(decoded.substring("offset:".length()));
            if (offset < 0) {
                throw invalidCursor();
            }
            return offset;
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private String encodeCursor(int offset) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        ("offset:" + offset).getBytes(StandardCharsets.UTF_8));
    }

    private BusinessException invalidCursor() {
        return new BusinessException(
                "INVALID_CURSOR",
                HttpStatus.BAD_REQUEST,
                "Cursor is invalid");
    }
}
