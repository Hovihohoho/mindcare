package com.mindcare.emotionservice.healthmetric.repository;

import com.mindcare.emotionservice.healthmetric.entity.HealthMetricSyncRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HealthMetricSyncRequestRepository extends JpaRepository<HealthMetricSyncRequestEntity, UUID> {

    Optional<HealthMetricSyncRequestEntity> findByUserIdAndSourceTypeAndIdempotencyKey(
            UUID userId,
            String sourceType,
            String idempotencyKey
    );
}
