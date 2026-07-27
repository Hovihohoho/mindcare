package com.mindcare.bookingservice.booking.dto;

import com.mindcare.bookingservice.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID scheduleId,
        @Size(max = 2000) String note,
        PaymentMethod paymentMethod) {
}
