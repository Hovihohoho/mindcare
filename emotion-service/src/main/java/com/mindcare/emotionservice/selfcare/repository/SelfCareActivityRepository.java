package com.mindcare.emotionservice.selfcare.repository;

import com.mindcare.emotionservice.selfcare.entity.SelfCareActivityEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelfCareActivityRepository extends JpaRepository<SelfCareActivityEntity, UUID> {
    Optional<SelfCareActivityEntity> findByIdAndPlanUserId(UUID id, UUID userId);
}
