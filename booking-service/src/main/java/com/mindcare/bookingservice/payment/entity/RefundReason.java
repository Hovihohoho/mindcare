package com.mindcare.bookingservice.payment.entity;

public enum RefundReason {
    USER_CANCELED_IN_POLICY,
    EXPERT_CANCELED,
    SYSTEM_CANCELED,
    EXPERT_NO_SHOW,
    LATE_PAYMENT_SUCCESS,
    DUPLICATE_PAYMENT,
    ADMIN_APPROVED
}
