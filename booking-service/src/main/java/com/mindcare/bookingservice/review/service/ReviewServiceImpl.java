package com.mindcare.bookingservice.review.service;

import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingAccessService;
import com.mindcare.bookingservice.booking.service.BookingAccessSnapshot;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.review.dto.CreateReviewRequest;
import com.mindcare.bookingservice.review.dto.ReviewResponse;
import com.mindcare.bookingservice.review.entity.ExpertReview;
import com.mindcare.bookingservice.review.mapper.ReviewMapper;
import com.mindcare.bookingservice.review.repository.ExpertReviewRepository;
import com.mindcare.bookingservice.review.repository.RatingAggregate;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import com.mindcare.bookingservice.shared.dto.PageCursor;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import com.mindcare.bookingservice.shared.web.CursorCodec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ExpertReviewRepository reviewRepository;
    private final BookingAccessService bookingAccessService;
    private final ReviewMapper reviewMapper;
    private final CursorCodec cursorCodec;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public ReviewResponse create(
            UUID userId,
            UUID bookingId,
            CreateReviewRequest request) {
        BookingAccessSnapshot access = bookingAccessService.getRequiredForUpdate(bookingId);
        if (!access.userId().equals(userId)) {
            throw new ResourceNotFoundException();
        }
        if (access.status() != BookingStatus.COMPLETED) {
            throw new BusinessException(
                    "BOOKING_NOT_REVIEWABLE",
                    HttpStatus.CONFLICT,
                    "Only completed bookings can be reviewed");
        }
        if (reviewRepository.findByBookingIdAndDeletedAtIsNull(bookingId).isPresent()) {
            throw new BusinessException(
                    "REVIEW_ALREADY_EXISTS",
                    HttpStatus.CONFLICT,
                    "Booking has already been reviewed");
        }

        ExpertReview review = reviewRepository.save(ExpertReview.create(
                bookingId,
                userId,
                access.expertUserId(),
                request.rating(),
                request.comment()));
        RatingAggregate aggregate =
                reviewRepository.aggregateForExpert(access.expertUserId());
        outboxService.append(
                "REVIEW",
                review.getId(),
                "review.created",
                Map.of(
                        "reviewId", review.getId(),
                        "bookingId", bookingId,
                        "expertUserId", access.expertUserId(),
                        "rating", request.rating()));
        outboxService.append(
                "EXPERT_RATING",
                access.expertUserId(),
                "expert.rating-projection-updated",
                Map.of(
                        "expertUserId", access.expertUserId(),
                        "ratingAverage", aggregate.getRatingAverage(),
                        "reviewCount", aggregate.getReviewCount()));
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ReviewResponse> listForExpert(
            UUID expertUserId,
            String cursor,
            int limit) {
        PageCursor decoded = cursorCodec.decode(cursor);
        int pageSize = Math.max(1, Math.min(limit, 100));
        List<ExpertReview> source = reviewRepository.findPublicHistory(
                expertUserId,
                decoded.createdAt(),
                decoded.id(),
                PageRequest.of(0, pageSize + 1));
        boolean hasMore = source.size() > pageSize;
        List<ExpertReview> page = hasMore ? source.subList(0, pageSize) : source;
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            ExpertReview last = page.get(page.size() - 1);
            nextCursor = cursorCodec.encode(last.getCreatedAt(), last.getId());
        }
        return new CursorPageResponse<>(
                page.stream().map(reviewMapper::toResponse).toList(),
                nextCursor,
                hasMore);
    }
}
