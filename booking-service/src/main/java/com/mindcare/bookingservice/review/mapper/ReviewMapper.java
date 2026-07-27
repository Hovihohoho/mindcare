package com.mindcare.bookingservice.review.mapper;

import com.mindcare.bookingservice.review.dto.ReviewResponse;
import com.mindcare.bookingservice.review.entity.ExpertReview;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewMapper {

    ReviewResponse toResponse(ExpertReview review);
}
