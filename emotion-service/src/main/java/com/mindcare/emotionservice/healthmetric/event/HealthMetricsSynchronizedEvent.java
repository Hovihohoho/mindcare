package com.mindcare.emotionservice.healthmetric.event;

import java.util.UUID;

public record HealthMetricsSynchronizedEvent(UUID userId, int changedMetricCount) {
}
