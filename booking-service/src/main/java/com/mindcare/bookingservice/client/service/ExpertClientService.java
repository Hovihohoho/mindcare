package com.mindcare.bookingservice.client.service;

import com.mindcare.bookingservice.client.dto.ExpertClientProfileResponse;
import java.util.UUID;

public interface ExpertClientService {

    ExpertClientProfileResponse getBySchedule(UUID expertUserId, UUID scheduleId);

    ExpertClientProfileResponse getByUser(UUID expertUserId, UUID userId);
}
