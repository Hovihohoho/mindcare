package com.mindcare.emotionservice.risk.service;

import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import com.mindcare.emotionservice.risk.entity.PsychologicalAlertLogEntity;
import com.mindcare.emotionservice.risk.mapper.RiskAlertMapper;
import com.mindcare.emotionservice.risk.repository.PsychologicalAlertLogRepository;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceImplTest {

    @Mock
    private PsychologicalAlertLogRepository repository;
    @Mock
    private RiskAlertMapper mapper;
    @Mock
    private AssessmentService assessmentService;

    private RiskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RiskServiceImpl(
                repository,
                mapper,
                assessmentService,
                new CursorCodec(),
                Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void analyzeRiskCreatesHighAlertForExtremeScreeningResult() {
        UUID userId = UUID.randomUUID();
        AssessmentResultResponse result = new AssessmentResultResponse(
                UUID.randomUUID(), AssessmentCode.PHQ_9, 1, 22, "EXTREME", "notice", List.of(), null
        );
        when(assessmentService.getAssessmentHistory(eq(userId), any(), any(), eq(null), anyInt()))
                .thenReturn(new CursorPageResponse<>(List.of(result), null, false));
        when(repository.existsByUserIdAndDeduplicationKeyAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                eq(userId), anyString(), any()
        )).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new RiskAlertResponse(
                null, "HIGH", "safe", false, null
        ));

        assertTrue(service.analyzeRisk(userId).isPresent());
        verify(repository).saveAndFlush(any(PsychologicalAlertLogEntity.class));
    }
}
