package com.mindcare.bookingservice.integration.auth.service;

import com.mindcare.bookingservice.integration.auth.ExpertDirectoryGateway;
import com.mindcare.bookingservice.integration.auth.ExpertDirectoryItem;
import com.mindcare.bookingservice.integration.auth.ExpertDirectoryPage;
import com.mindcare.bookingservice.integration.auth.ExpertDirectoryQuery;
import com.mindcare.bookingservice.integration.auth.dto.ExpertSummaryResponse;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpertDirectoryServiceImpl implements ExpertDirectoryService {

    private final ExpertDirectoryGateway expertDirectoryGateway;

    @Override
    public CursorPageResponse<ExpertSummaryResponse> listBookable(
            String keyword,
            String specialty,
            String cursor,
            int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        ExpertDirectoryPage page = expertDirectoryGateway.listBookable(
                new ExpertDirectoryQuery(
                        normalize(keyword),
                        normalize(specialty),
                        normalize(cursor),
                        normalizedLimit));
        return new CursorPageResponse<>(
                page.items().stream().map(this::toResponse).toList(),
                page.nextCursor(),
                page.hasMore());
    }

    private ExpertSummaryResponse toResponse(ExpertDirectoryItem item) {
        return new ExpertSummaryResponse(
                item.expertUserId(),
                item.displayName(),
                item.headline(),
                item.specialties(),
                item.yearsOfExperience(),
                item.consultationFee(),
                item.currency(),
                item.averageRating(),
                item.reviewCount());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
