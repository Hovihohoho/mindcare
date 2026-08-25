package com.mindcare.emotionservice.selfcare.repository;

import com.mindcare.emotionservice.selfcare.entity.SelfCareCompletionEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelfCareCompletionRepository extends JpaRepository<SelfCareCompletionEntity, UUID> {
    List<SelfCareCompletionEntity> findByActivityPlanUserIdAndCompletedOnBetween(UUID userId, LocalDate from, LocalDate to);
    Optional<SelfCareCompletionEntity> findByActivityIdAndCompletedOn(UUID activityId, LocalDate date);
}
