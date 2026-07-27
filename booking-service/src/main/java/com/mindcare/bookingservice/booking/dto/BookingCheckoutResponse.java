package com.mindcare.bookingservice.booking.dto;

import com.mindcare.bookingservice.payment.dto.PaymentResponse;

public record BookingCheckoutResponse(
        BookingResponse booking,
        PaymentResponse payment,
        boolean paymentRequired) {
}
