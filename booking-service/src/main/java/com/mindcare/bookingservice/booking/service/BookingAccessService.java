package com.mindcare.bookingservice.booking.service;

import java.util.UUID;

public interface BookingAccessService {

    BookingAccessSnapshot getRequired(UUID bookingId);

    BookingAccessSnapshot getRequiredForUpdate(UUID bookingId);
}
