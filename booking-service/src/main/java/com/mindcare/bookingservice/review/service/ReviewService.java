package com.mindcare.bookingservice.review.service;

import com.mindcare.bookingservice.review.dto.CreateReviewRequest;
import com.mindcare.bookingservice.review.dto.ReviewResponse;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.util.UUID;

public interface ReviewService {

    ReviewResponse create(UUID userId, UUID bookingId, CreateReviewRequest request);

    CursorPageResponse<ReviewResponse> listForExpert(
            UUID expertUserId,
            String cursor,
            int limit);
}
