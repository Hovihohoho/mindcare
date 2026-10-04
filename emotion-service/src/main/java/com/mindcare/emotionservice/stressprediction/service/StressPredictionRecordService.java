package com.mindcare.emotionservice.stressprediction.service;

import com.mindcare.emotionservice.stressprediction.dto.StoreStressPredictionRequest;
import com.mindcare.emotionservice.stressprediction.dto.StressPredictionRecordResponse;
import com.mindcare.emotionservice.stressprediction.entity.PmdataStressPredictionEntity;
import com.mindcare.emotionservice.stressprediction.repository.PmdataStressPredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StressPredictionRecordService {
    private final PmdataStressPredictionRepository repository;
    private final Clock clock;

    @Transactional
    public StressPredictionRecordResponse store(UUID userId, StoreStressPredictionRequest request) {
        PmdataStressPredictionEntity prediction = repository
                .findByUserIdAndFeatureDateAndModelVersion(userId, request.featureDate(), request.modelVersion())
                .orElseGet(() -> new PmdataStressPredictionEntity(userId, request));
        prediction.update(request);
        prediction = repository.saveAndFlush(prediction);
        return response(prediction);
    }

    @Transactional(readOnly = true)
    public Optional<StressPredictionRecordResponse> latest(UUID userId) {
        return repository.findFirstByUserIdOrderByFeatureDateDescUpdatedAtDesc(userId).map(this::response);
    }

    @Transactional
    public void markNotified(UUID predictionId, int score) {
        repository.findById(predictionId).ifPresent(prediction -> {
            if (prediction.getNotifiedScore() == null || score > prediction.getNotifiedScore()) {
                prediction.markNotified(score, OffsetDateTime.now(clock));
            }
        });
    }

    /** A conservative three-day trend is used only to invite a self check-in. */
    @Transactional(readOnly = true)
    public boolean recommendsMorningCheckIn(UUID userId, LocalDate today) {
        LocalDate latestFeatureDate = today.minusDays(1);
        LocalDate firstFeatureDate = latestFeatureDate.minusDays(2);
        Map<LocalDate, PmdataStressPredictionEntity> latestByDay = repository
                .findByUserIdAndFeatureDateBetweenOrderByFeatureDateDescUpdatedAtDesc(
                        userId, firstFeatureDate, latestFeatureDate)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PmdataStressPredictionEntity::getFeatureDate,
                        value -> value,
                        (first, ignored) -> first));

        return java.util.stream.Stream.of(latestFeatureDate, latestFeatureDate.minusDays(1),
                        latestFeatureDate.minusDays(2))
                .map(latestByDay::get)
                .allMatch(prediction -> prediction != null && prediction.getStressScore() >= 4);
    }

    private StressPredictionRecordResponse response(PmdataStressPredictionEntity value) {
        return new StressPredictionRecordResponse(
                value.getId(), value.getFeatureDate(), value.getTimezone(), value.getFeatureVersion(),
                value.getStressScore(), value.getRelativeLevel(), value.getConfidence(), value.getModelVersion(),
                value.getAlertLevel(), value.isNotificationRequired(), value.getNotifiedAt(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}
