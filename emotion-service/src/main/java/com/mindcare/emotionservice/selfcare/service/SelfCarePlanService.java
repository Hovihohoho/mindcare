package com.mindcare.emotionservice.selfcare.service;

import com.mindcare.emotionservice.selfcare.dto.*;
import java.time.LocalDate;
import java.util.UUID;

public interface SelfCarePlanService {
    SelfCarePlanResponse get(UUID userId, LocalDate today);
    SelfCarePlanResponse upsert(UUID userId, UpsertSelfCarePlanRequest request, LocalDate today);
    SelfCarePlanResponse complete(UUID userId, UUID activityId, LocalDate date, LocalDate today);
    SelfCarePlanResponse undo(UUID userId, UUID activityId, LocalDate date, LocalDate today);
}
