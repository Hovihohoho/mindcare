package com.mindcare.emotionservice.selfcare.dto;

import com.mindcare.emotionservice.selfcare.entity.SelfCareGoal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record UpsertSelfCarePlanRequest(
        @NotNull SelfCareGoal goal,
        @NotEmpty @Size(max = 5) List<@Valid ActivityRequest> activities
) {
    public record ActivityRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,50}") String activityCode,
            @NotBlank @Size(max = 160) String title,
            @Min(1) @Max(7) int targetPerWeek
    ) {}
}
