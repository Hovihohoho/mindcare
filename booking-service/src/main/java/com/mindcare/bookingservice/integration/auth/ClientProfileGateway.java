package com.mindcare.bookingservice.integration.auth;

import java.util.UUID;

public interface ClientProfileGateway {

    ClientProfile getClientProfile(UUID userId);
}
