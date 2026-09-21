package com.mindcare.emotionservice.selfcare.dto;

import com.mindcare.emotionservice.selfcare.entity.SelfCareGoal;

import java.util.List;

public record SelfCarePlanTemplateResponse(
        String templateCode,
        String templateVersion,
        SelfCareGoal goal,
        String title,
        String description,
        List<ActivityTemplateResponse> activities,
        String sourceTitle,
        String sourceUrl,
        String implementationSourceUrl,
        String limitation
) {
    public record ActivityTemplateResponse(String activityCode, String title, int targetPerWeek) {}
}
