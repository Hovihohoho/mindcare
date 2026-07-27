package com.mindcare.emotionservice.risk.service;

import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RiskService {

    Optional<RiskAlertResponse> analyzeRisk(UUID userId);

    RiskAlertResponse getAlert(UUID userId, UUID alertId);

    CursorPageResponse<RiskAlertResponse> getAlerts(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    );

    void markAlertNotified(UUID alertId);
}
