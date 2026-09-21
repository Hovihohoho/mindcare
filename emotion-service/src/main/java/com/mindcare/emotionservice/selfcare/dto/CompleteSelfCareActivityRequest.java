package com.mindcare.emotionservice.selfcare.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CompleteSelfCareActivityRequest(@NotNull LocalDate completedOn) {}
