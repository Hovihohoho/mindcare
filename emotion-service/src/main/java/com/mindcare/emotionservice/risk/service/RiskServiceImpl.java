package com.mindcare.emotionservice.risk.service;

import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import com.mindcare.emotionservice.risk.entity.PsychologicalAlertLogEntity;
import com.mindcare.emotionservice.risk.mapper.RiskAlertMapper;
import com.mindcare.emotionservice.risk.repository.PsychologicalAlertLogRepository;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import com.mindcare.emotionservice.shared.util.ServiceValidator;
import com.mindcare.emotionservice.healthmetric.service.HealthBenchmarkService;
import com.mindcare.emotionservice.healthmetric.dto.HealthBenchmarkEvaluation;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RiskServiceImpl implements RiskService {

    private static final String RULE_VERSION = "risk-signal-v2";

    private final PsychologicalAlertLogRepository repository;
    private final RiskAlertMapper mapper;
    private final AssessmentService assessmentService;
    private final CursorCodec cursorCodec;
    private final Clock clock;
    private final HealthBenchmarkService healthBenchmarkService;

    public RiskServiceImpl(
            PsychologicalAlertLogRepository repository,
            RiskAlertMapper mapper,
            AssessmentService assessmentService,
            CursorCodec cursorCodec,
            Clock clock,
            HealthBenchmarkService healthBenchmarkService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.assessmentService = assessmentService;
        this.cursorCodec = cursorCodec;
        this.clock = clock;
        this.healthBenchmarkService = healthBenchmarkService;
    }

    @Override
    @Transactional
    public List<RiskAlertResponse> analyzeHealthBenchmarks(UUID userId) {
        ServiceValidator.requireUserId(userId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        return healthBenchmarkService.evaluate(userId).stream()
                .filter(HealthBenchmarkEvaluation::alertTriggered)
                .filter(evaluation -> !repository.existsByUserIdAndDeduplicationKeyAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                        userId, healthDeduplicationKey(evaluation), now.minus(healthCooldown(evaluation))))
                .map(evaluation -> PsychologicalAlertLogEntity.healthBenchmark(
                        userId, healthAlertLevel(evaluation), evaluation.message(), evaluation.reasonCode(),
                        healthDeduplicationKey(evaluation), evaluation.policyKey(), evaluation.policyVersion(),
                        evaluation.sourceUrl(), evaluation.metricType(), evaluation.observedValue(), evaluation.unit(),
                        evaluation.recommendedPlanTemplateCode()))
                .map(repository::saveAndFlush)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public Optional<RiskAlertResponse> analyzeRisk(UUID userId) {
        ServiceValidator.requireUserId(userId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        CursorPageResponse<AssessmentResultResponse> results = assessmentService.getAssessmentHistory(
                userId,
                now.minusDays(30),
                now.plusNanos(1),
                null,
                1
        );
        if (results.items().isEmpty()) {
            return Optional.empty();
        }
        AssessmentResultResponse result = results.items().get(0);
        AlertDecision decision = decisionFor(result.riskSignals());
        if (decision == null) {
            return Optional.empty();
        }
        String deduplicationKey = RULE_VERSION + ':' + decision.alertLevel();
        if (repository.existsByUserIdAndDeduplicationKeyAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
                userId,
                deduplicationKey,
                now.minus(decision.cooldown())
        )) {
            return Optional.empty();
        }
        PsychologicalAlertLogEntity alert = new PsychologicalAlertLogEntity(
                userId,
                decision.alertLevel(),
                decision.safeMessage(),
                RULE_VERSION,
                decision.reasonCode(),
                result.resultId(),
                deduplicationKey
        );
        return Optional.of(mapper.toResponse(repository.saveAndFlush(alert)));
    }

    @Override
    public RiskAlertResponse getAlert(UUID userId, UUID alertId) {
        ServiceValidator.requireUserId(userId);
        if (alertId == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "alertId must not be null");
        }
        return repository.findByIdAndUserIdAndDeletedAtIsNull(alertId, userId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Risk alert"));
    }

    @Override
    public CursorPageResponse<RiskAlertResponse> getAlerts(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    ) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 365);
        ServiceValidator.validateLimit(limit);
        String scope = "risk-alert:" + userId + ':' + from + ':' + to;
        CursorCodec.CursorPosition position = cursorCodec.decode(cursor, scope);
        List<PsychologicalAlertLogEntity> entities = repository.findHistory(
                userId,
                from,
                to,
                position.timestamp() != null,
                position.timestamp(),
                position.id(),
                PageRequest.of(0, limit + 1)
        );
        boolean hasMore = entities.size() > limit;
        List<PsychologicalAlertLogEntity> page = entities.subList(0, Math.min(limit, entities.size()));
        String nextCursor = hasMore
                ? cursorCodec.encode(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId(), scope)
                : null;
        return new CursorPageResponse<>(page.stream().map(mapper::toResponse).toList(), nextCursor, hasMore);
    }

    @Override
    @Transactional
    public void markAlertNotified(UUID alertId) {
        if (alertId == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "alertId must not be null");
        }
        PsychologicalAlertLogEntity alert = repository.findByIdAndDeletedAtIsNull(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Risk alert"));
        if (!Boolean.TRUE.equals(alert.getNotified())) {
            alert.markNotified(OffsetDateTime.now(clock));
            repository.save(alert);
        }
    }

    private AlertDecision decisionFor(List<AssessmentResultResponse.RiskSignalResponse> signals) {
        if (signals == null || signals.stream().noneMatch(signal -> "SELF_HARM_ITEM".equals(signal.type()))) {
            return null;
        }
        return new AlertDecision(
                "HIGH",
                "PHQ9_SELF_HARM_ITEM",
                "Câu trả lời gần đây cho thấy bạn có thể cần được hỗ trợ an toàn. Nếu bạn đang có ý định tự làm hại mình hoặc không thể giữ an toàn, hãy liên hệ dịch vụ cấp cứu tại địa phương, đến khoa cấp cứu gần nhất và nhờ một người tin cậy ở bên.",
                Duration.ofHours(24)
        );
    }

    private String healthDeduplicationKey(HealthBenchmarkEvaluation evaluation) {
        return "health-benchmark-v1:" + evaluation.policyKey() + ':' + evaluation.reasonCode();
    }

    private Duration healthCooldown(HealthBenchmarkEvaluation evaluation) {
        return Set.of("SLEEP_DURATION", "STEP_COUNT").contains(evaluation.metricType())
                ? Duration.ofDays(7) : Duration.ofHours(24);
    }

    private String healthAlertLevel(HealthBenchmarkEvaluation evaluation) {
        return switch (evaluation.metricType()) {
            case "SPO2" -> "ELEVATED";
            case "HEART_RATE" -> "CHECK";
            default -> "WELLNESS";
        };
    }

    private record AlertDecision(
            String alertLevel,
            String reasonCode,
            String safeMessage,
            Duration cooldown
    ) {
    }
}
