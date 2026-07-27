package com.mindcare.bookingservice.integration.auth;

import java.util.UUID;

public interface ExpertProfileGateway {

    ExpertBookingProfile getBookingProfile(UUID expertUserId);
}
