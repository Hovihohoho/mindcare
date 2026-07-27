package com.mindcare.bookingservice.booking.entity;

public enum BookingStatus {
    PAYMENT_PENDING,
    CONFIRMED,
    CANCELLATION_PENDING,
    CANCELED,
    COMPLETED,
    USER_NO_SHOW,
    EXPERT_NO_SHOW,
    PAYMENT_FAILED,
    EXPIRED
}
