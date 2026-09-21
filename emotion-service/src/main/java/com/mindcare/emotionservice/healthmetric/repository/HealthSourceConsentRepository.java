package com.mindcare.emotionservice.healthmetric.repository;

import com.mindcare.emotionservice.healthmetric.entity.HealthSourceConsentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthSourceConsentRepository extends JpaRepository<HealthSourceConsentEntity, UUID> {
    Optional<HealthSourceConsentEntity> findByUserIdAndSourceType(UUID userId, String sourceType);
}
