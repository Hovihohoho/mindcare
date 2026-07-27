package com.mindcare.bookingservice.booking.service;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface BookingPaymentLifecycleService {

    boolean confirmPaymentIfActive(UUID bookingId, OffsetDateTime paidAt);

    void failPayment(UUID bookingId);

    void expireBooking(UUID bookingId);

    void markRefunded(UUID bookingId);
}
