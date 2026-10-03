package com.mindcare.emotionservice.stressprediction.event;

import java.util.UUID;

public record StressPredictionAlertEvent(UUID predictionId, UUID userId, int stressScore, String alertLevel) {
}
