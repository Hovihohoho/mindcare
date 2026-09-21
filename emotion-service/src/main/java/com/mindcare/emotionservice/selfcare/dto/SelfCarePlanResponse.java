package com.mindcare.emotionservice.selfcare.dto;

import com.mindcare.emotionservice.selfcare.entity.SelfCareGoal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SelfCarePlanResponse(
        UUID id,
        SelfCareGoal goal,
        String templateCode,
        String templateVersion,
        String sourceUrl,
        LocalDate weekStartedOn,
        int completedThisWeek,
        int targetThisWeek,
        List<ActivityResponse> activities,
        OffsetDateTime updatedAt
) {
    public record ActivityResponse(
            UUID id, String activityCode, String title, int targetPerWeek,
            int completedThisWeek, boolean completedToday
    ) {}
}
