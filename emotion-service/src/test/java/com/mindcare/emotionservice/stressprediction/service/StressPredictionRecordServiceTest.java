package com.mindcare.emotionservice.stressprediction.service;

import com.mindcare.emotionservice.stressprediction.dto.StoreStressPredictionRequest;
import com.mindcare.emotionservice.stressprediction.entity.PmdataStressPredictionEntity;
import com.mindcare.emotionservice.stressprediction.repository.PmdataStressPredictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StressPredictionRecordServiceTest {
    @Mock private PmdataStressPredictionRepository repository;

    @Test
    void scoreThreeIsStoredWithoutImmediateNotification() {
        var service = storingService();
        var response = service.store(UUID.randomUUID(), request(3));

        assertThat(response.alertLevel()).isEqualTo("MONITOR");
        assertThat(response.notificationRequired()).isFalse();
    }

    @Test
    void scoreFourIsStoredWithoutAnImmediateAlert() {
        UUID userId = UUID.randomUUID();
        var service = storingService();
        var response = service.store(userId, request(4));

        assertThat(response.alertLevel()).isEqualTo("ELEVATED");
        assertThat(response.notificationRequired()).isTrue();
    }

    @Test
    void escalationFromFourToFiveRequiresAnotherNotification() {
        var prediction = new PmdataStressPredictionEntity(UUID.randomUUID(), request(4));
        prediction.markNotified(4, OffsetDateTime.now());
        prediction.update(request(5));

        assertThat(prediction.requiresNewNotification()).isTrue();
        assertThat(prediction.getAlertLevel()).isEqualTo("HIGH");
    }

    @Test
    void recommendsMorningCheckInOnlyForThreeConsecutiveHighDaysEndingYesterday() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 9, 25);
        when(repository.findByUserIdAndFeatureDateBetweenOrderByFeatureDateDescUpdatedAtDesc(
                userId, today.minusDays(3), today.minusDays(1)))
                .thenReturn(List.of(
                        new PmdataStressPredictionEntity(userId, request(today.minusDays(1), 4)),
                        new PmdataStressPredictionEntity(userId, request(today.minusDays(2), 5)),
                        new PmdataStressPredictionEntity(userId, request(today.minusDays(3), 4))));

        assertThat(service().recommendsMorningCheckIn(userId, today)).isTrue();
    }

    @Test
    void doesNotRecommendMorningCheckInWhenTheWindowIsMixedOrIncomplete() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 9, 25);
        when(repository.findByUserIdAndFeatureDateBetweenOrderByFeatureDateDescUpdatedAtDesc(
                userId, today.minusDays(3), today.minusDays(1)))
                .thenReturn(List.of(
                        new PmdataStressPredictionEntity(userId, request(today.minusDays(1), 4)),
                        new PmdataStressPredictionEntity(userId, request(today.minusDays(2), 3))));

        assertThat(service().recommendsMorningCheckIn(userId, today)).isFalse();
    }

    private StressPredictionRecordService storingService() {
        when(repository.findByUserIdAndFeatureDateAndModelVersion(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(PmdataStressPredictionEntity.class))).thenAnswer(invocation -> {
            PmdataStressPredictionEntity entity = invocation.getArgument(0);
            var id = PmdataStressPredictionEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(entity, UUID.randomUUID());
            return entity;
        });
        return new StressPredictionRecordService(repository, Clock.systemUTC());
    }

    private StressPredictionRecordService service() {
        return new StressPredictionRecordService(repository, Clock.systemUTC());
    }

    private StoreStressPredictionRequest request(int score) {
        return request(LocalDate.of(2026, 9, 22), score);
    }

    private StoreStressPredictionRequest request(LocalDate featureDate, int score) {
        return new StoreStressPredictionRequest(
                featureDate, "Asia/Ho_Chi_Minh", "pmdata-features-v2",
                score, "CLIENT_VALUE_IS_NOT_TRUSTED", new BigDecimal("0.70"), "stress-classifier-v3");
    }
}
