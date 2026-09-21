package com.mindcare.emotionservice.selfcare.service;

import com.mindcare.emotionservice.selfcare.dto.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SelfCarePlanService {
    SelfCarePlanResponse get(UUID userId, LocalDate today);
    SelfCarePlanResponse upsert(UUID userId, UpsertSelfCarePlanRequest request, LocalDate today);
    SelfCarePlanResponse complete(UUID userId, UUID activityId, LocalDate date, LocalDate today);
    SelfCarePlanResponse undo(UUID userId, UUID activityId, LocalDate date, LocalDate today);
    List<SelfCarePlanTemplateResponse> templates();
    List<SelfCarePlanRecommendationResponse> recommendations(UUID userId);
    SelfCarePlanResponse applyTemplate(UUID userId, String templateCode, LocalDate today);
}
