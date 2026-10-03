package com.mindcare.emotionservice.stressprediction.repository;

import com.mindcare.emotionservice.stressprediction.entity.PmdataStressPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PmdataStressPredictionRepository extends JpaRepository<PmdataStressPredictionEntity, UUID> {
    Optional<PmdataStressPredictionEntity> findByUserIdAndFeatureDateAndModelVersion(
            UUID userId, LocalDate featureDate, String modelVersion);

    Optional<PmdataStressPredictionEntity> findFirstByUserIdOrderByFeatureDateDescUpdatedAtDesc(UUID userId);

    List<PmdataStressPredictionEntity> findByUserIdAndFeatureDateBetweenOrderByFeatureDateDescUpdatedAtDesc(
            UUID userId, LocalDate from, LocalDate to);
}
