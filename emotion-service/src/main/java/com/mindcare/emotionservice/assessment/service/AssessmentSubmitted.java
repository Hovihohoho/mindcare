package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import java.util.UUID;

/** In-process event only; never serialize assessment answers to a broker or log. */
public record AssessmentSubmitted(UUID userId, AssessmentResultResponse result) {}
