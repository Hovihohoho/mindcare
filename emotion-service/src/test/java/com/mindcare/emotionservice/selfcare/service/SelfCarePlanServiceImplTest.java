package com.mindcare.emotionservice.selfcare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindcare.emotionservice.selfcare.dto.UpsertSelfCarePlanRequest;
import com.mindcare.emotionservice.selfcare.entity.SelfCareGoal;
import com.mindcare.emotionservice.selfcare.repository.*;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelfCarePlanServiceImplTest {
    private SelfCarePlanRepository plans;
    private SelfCareCompletionRepository completions;
    private SelfCarePlanServiceImpl service;

    @BeforeEach
    void setUp() {
        plans = mock(SelfCarePlanRepository.class);
        completions = mock(SelfCareCompletionRepository.class);
        service = new SelfCarePlanServiceImpl(plans, mock(SelfCareActivityRepository.class), completions);
    }

    @Test
    void createsPlanForAuthenticatedOwnerWithWeeklyTargets() {
        UUID userId = UUID.randomUUID();
        var request = new UpsertSelfCarePlanRequest(SelfCareGoal.REDUCE_STRESS, List.of(
                new UpsertSelfCarePlanRequest.ActivityRequest("BREATHING_5_MIN", "Thở chậm 5 phút", 5),
                new UpsertSelfCarePlanRequest.ActivityRequest("SHORT_WALK", "Đi bộ nhẹ", 3)));
        when(plans.findByUserId(userId)).thenReturn(Optional.empty());
        when(plans.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(completions.findByActivityPlanUserIdAndCompletedOnBetween(eq(userId), any(), any()))
                .thenReturn(List.of());

        var response = service.upsert(userId, request, LocalDate.of(2026, 8, 24));

        assertThat(response.goal()).isEqualTo(SelfCareGoal.REDUCE_STRESS);
        assertThat(response.activities()).hasSize(2);
        assertThat(response.targetThisWeek()).isEqualTo(8);
        verify(plans).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateActivityCodes() {
        var item = new UpsertSelfCarePlanRequest.ActivityRequest("SHORT_WALK", "Đi bộ nhẹ", 3);
        var request = new UpsertSelfCarePlanRequest(SelfCareGoal.BUILD_BALANCE, List.of(item, item));

        assertThatThrownBy(() -> service.upsert(UUID.randomUUID(), request, LocalDate.of(2026, 8, 24)))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(plans);
    }
}
