package com.mindcare.emotionservice.selfcare.repository;

import com.mindcare.emotionservice.selfcare.entity.SelfCarePlanEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelfCarePlanRepository extends JpaRepository<SelfCarePlanEntity, UUID> {
    @EntityGraph(attributePaths = "activities")
    Optional<SelfCarePlanEntity> findByUserId(UUID userId);
}
