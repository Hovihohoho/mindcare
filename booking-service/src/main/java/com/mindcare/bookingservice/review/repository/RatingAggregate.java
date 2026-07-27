package com.mindcare.bookingservice.review.repository;

import java.math.BigDecimal;

public interface RatingAggregate {

    BigDecimal getRatingAverage();

    long getReviewCount();
}
