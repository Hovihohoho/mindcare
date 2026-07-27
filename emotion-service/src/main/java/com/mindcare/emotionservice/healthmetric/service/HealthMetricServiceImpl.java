package com.mindcare.emotionservice.healthmetric.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricItemRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricTrendPointResponse;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricSyncRequestEntity;
import com.mindcare.emotionservice.healthmetric.mapper.HealthMetricMapper;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricSyncRequestRepository;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceConflictException;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import com.mindcare.emotionservice.shared.util.RequestHasher;
import com.mindcare.emotionservice.shared.util.ServiceValidator;
import com.mindcare.emotionservice.shared.util.TrendBucket;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HealthMetricServiceImpl implements HealthMetricService {

    private static final int MAX_BATCH_SIZE = 100;
    private static final Set<String> SOURCE_TYPES = Set.of("APPLE_HEALTH", "GOOGLE_HEALTH", "MANUAL");

    private final HealthMetricRepository repository;
    private final HealthMetricSyncRequestRepository syncRequestRepository;
    private final HealthMetricMapper mapper;
    private final CursorCodec cursorCodec;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public HealthMetricServiceImpl(
            HealthMetricRepository repository,
            HealthMetricSyncRequestRepository syncRequestRepository,
            HealthMetricMapper mapper,
            CursorCodec cursorCodec,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.syncRequestRepository = syncRequestRepository;
        this.mapper = mapper;
        this.cursorCodec = cursorCodec;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public HealthMetricBatchResponse synchronizeMetrics(
            UUID userId,
            String idempotencyKey,
            HealthMetricBatchRequest request
    ) {
        ServiceValidator.requireUserId(userId);
        String key = ServiceValidator.requireText(idempotencyKey, "idempotencyKey", 255);
        if (request == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "request must not be null");
        }
        String sourceType = normalizeSource(request.sourceType());
        String requestHash = hashRequest(request, sourceType);
        var previous = syncRequestRepository.findByUserIdAndSourceTypeAndIdempotencyKey(userId, sourceType, key);
        if (previous.isPresent()) {
            if (!previous.get().getRequestHash().equals(requestHash)) {
                throw new ResourceConflictException(
                        "IDEMPOTENCY_CONFLICT",
                        "idempotencyKey was already used with a different payload"
                );
            }
            return readStoredResponse(previous.get());
        }

        if (request.items() == null || request.items().isEmpty() || request.items().size() > MAX_BATCH_SIZE) {
            throw new InvalidRequestException(
                    "INVALID_BATCH_SIZE",
                    "items must contain between 1 and " + MAX_BATCH_SIZE + " entries"
            );
        }

        List<PreparedMetric> preparedMetrics = request.items().stream()
                .map(item -> prepareMetric(item, sourceType))
                .toList();
        Set<String> seenSamples = new HashSet<>();
        List<HealthMetricEntity> newEntities = new ArrayList<>();
        int duplicateCount = 0;
        for (PreparedMetric prepared : preparedMetrics) {
            String externalSampleId = prepared.externalSampleId();
            boolean duplicateInBatch = externalSampleId != null && !seenSamples.add(externalSampleId);
            boolean duplicateInDatabase = externalSampleId != null
                    && repository.existsByUserIdAndSourceTypeAndExternalSampleIdAndDeletedAtIsNull(
                    userId,
                    sourceType,
                    externalSampleId
            );
            if (duplicateInBatch || duplicateInDatabase) {
                duplicateCount++;
                continue;
            }
            newEntities.add(new HealthMetricEntity(
                    userId,
                    prepared.metricType(),
                    prepared.value(),
                    prepared.unit(),
                    sourceType,
                    externalSampleId,
                    prepared.recordedAt()
            ));
        }

        List<HealthMetricEntity> saved = repository.saveAllAndFlush(newEntities);
        HealthMetricBatchResponse response = new HealthMetricBatchResponse(
                saved.size(),
                duplicateCount,
                saved.stream().map(HealthMetricEntity::getId).toList()
        );
        syncRequestRepository.save(new HealthMetricSyncRequestEntity(
                userId,
                sourceType,
                key,
                requestHash,
                objectMapper.valueToTree(response)
        ));
        return response;
    }

    @Override
    public CursorPageResponse<HealthMetricResponse> getMetrics(
            UUID userId,
            String metricType,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    ) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 365);
        ServiceValidator.validateLimit(limit);
        String normalizedMetricType = normalizeMetricType(metricType);
        String scope = "health:" + userId + ':' + normalizedMetricType + ':' + from + ':' + to;
        CursorCodec.CursorPosition position = cursorCodec.decode(cursor, scope);
        List<HealthMetricEntity> entities = repository.findHistory(
                userId,
                normalizedMetricType,
                from,
                to,
                position.timestamp() != null,
                position.timestamp(),
                position.id(),
                PageRequest.of(0, limit + 1)
        );
        boolean hasMore = entities.size() > limit;
        List<HealthMetricEntity> page = entities.subList(0, Math.min(limit, entities.size()));
        String nextCursor = hasMore
                ? cursorCodec.encode(page.get(page.size() - 1).getRecordedAt(), page.get(page.size() - 1).getId(), scope)
                : null;
        return new CursorPageResponse<>(page.stream().map(mapper::toResponse).toList(), nextCursor, hasMore);
    }

    @Override
    public List<HealthMetricTrendPointResponse> getMetricTrends(
            UUID userId,
            String metricType,
            OffsetDateTime from,
            OffsetDateTime to,
            String bucket,
            ZoneId timezone
    ) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 730);
        if (timezone == null) {
            throw new InvalidRequestException("INVALID_TIMEZONE", "timezone must not be null");
        }
        String normalizedMetricType = normalizeMetricType(metricType);
        TrendBucket trendBucket = TrendBucket.parse(bucket);
        List<HealthMetricEntity> metrics = repository
                .findByUserIdAndMetricTypeAndDeletedAtIsNullAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
                        userId,
                        normalizedMetricType,
                        from,
                        to
                );
        Map<OffsetDateTime, ValueAccumulator> groups = new LinkedHashMap<>();
        metrics.forEach(metric -> groups
                .computeIfAbsent(trendBucket.startOf(metric.getRecordedAt(), timezone), ignored -> new ValueAccumulator())
                .add(metric.getMetricValue()));
        List<HealthMetricTrendPointResponse> response = new ArrayList<>();
        groups.forEach((periodStart, accumulator) -> response.add(new HealthMetricTrendPointResponse(
                periodStart,
                trendBucket.endOf(periodStart, timezone),
                normalizedMetricType,
                accumulator.average(),
                accumulator.count,
                canonicalUnit(normalizedMetricType)
        )));
        return List.copyOf(response);
    }

    private PreparedMetric prepareMetric(HealthMetricItemRequest item, String sourceType) {
        if (item == null || item.value() == null || item.recordedAt() == null) {
            throw new InvalidRequestException("INVALID_HEALTH_METRIC", "metric value and recordedAt are required");
        }
        String metricType = normalizeMetricType(item.metricType());
        String externalSampleId = normalizeOptionalText(item.externalSampleId(), 255);
        if (!"MANUAL".equals(sourceType) && externalSampleId == null) {
            throw new InvalidRequestException(
                    "EXTERNAL_SAMPLE_ID_REQUIRED",
                    "externalSampleId is required for device health sources"
            );
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (item.recordedAt().isAfter(now.plusMinutes(5)) || item.recordedAt().isBefore(now.minusDays(30))) {
            throw new InvalidRequestException(
                    "INVALID_RECORDED_AT",
                    "recordedAt must be within the accepted 30-day synchronization window"
            );
        }
        return canonicalize(metricType, item.value(), item.unit(), item.recordedAt(), externalSampleId);
    }

    private PreparedMetric canonicalize(
            String metricType,
            BigDecimal value,
            String unit,
            OffsetDateTime recordedAt,
            String externalSampleId
    ) {
        String normalizedUnit = ServiceValidator.requireText(unit, "unit", 20).toLowerCase(Locale.ROOT);
        BigDecimal canonicalValue;
        String canonicalUnit = canonicalUnit(metricType);
        switch (metricType) {
            case "SLEEP_HOURS" -> {
                canonicalValue = switch (normalizedUnit) {
                    case "h", "hour", "hours" -> value;
                    case "min", "minute", "minutes" -> value.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                    default -> throw invalidUnit(metricType);
                };
                validateRange(canonicalValue, BigDecimal.ZERO, BigDecimal.valueOf(24), metricType);
            }
            case "HEART_RATE" -> {
                if (!"bpm".equals(normalizedUnit)) {
                    throw invalidUnit(metricType);
                }
                canonicalValue = value;
                validateRange(canonicalValue, BigDecimal.valueOf(20), BigDecimal.valueOf(250), metricType);
            }
            case "STEP_COUNT" -> {
                if (!Set.of("count", "step", "steps").contains(normalizedUnit)) {
                    throw invalidUnit(metricType);
                }
                if (value.stripTrailingZeros().scale() > 0) {
                    throw new InvalidRequestException("INVALID_METRIC_VALUE", "STEP_COUNT must be an integer");
                }
                canonicalValue = value;
                validateRange(canonicalValue, BigDecimal.ZERO, BigDecimal.valueOf(200_000), metricType);
            }
            default -> throw new InvalidRequestException("INVALID_METRIC_TYPE", "unsupported metricType");
        }
        return new PreparedMetric(metricType, canonicalValue, canonicalUnit, recordedAt, externalSampleId);
    }

    private void validateRange(BigDecimal value, BigDecimal minimum, BigDecimal maximum, String metricType) {
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new InvalidRequestException("INVALID_METRIC_VALUE", metricType + " is outside the ingest range");
        }
    }

    private InvalidRequestException invalidUnit(String metricType) {
        return new InvalidRequestException("INVALID_METRIC_UNIT", "unit is not supported for " + metricType);
    }

    private String normalizeSource(String sourceType) {
        String normalized = ServiceValidator.requireText(sourceType, "sourceType", 50).toUpperCase(Locale.ROOT);
        if (!SOURCE_TYPES.contains(normalized)) {
            throw new InvalidRequestException("INVALID_SOURCE_TYPE", "unsupported sourceType");
        }
        return normalized;
    }

    private String normalizeMetricType(String metricType) {
        String normalized = ServiceValidator.requireText(metricType, "metricType", 50).toUpperCase(Locale.ROOT);
        if (!Set.of("SLEEP_HOURS", "HEART_RATE", "STEP_COUNT").contains(normalized)) {
            throw new InvalidRequestException("INVALID_METRIC_TYPE", "unsupported metricType");
        }
        return normalized;
    }

    private String canonicalUnit(String metricType) {
        return switch (metricType) {
            case "SLEEP_HOURS" -> "h";
            case "HEART_RATE" -> "bpm";
            case "STEP_COUNT" -> "count";
            default -> throw new InvalidRequestException("INVALID_METRIC_TYPE", "unsupported metricType");
        };
    }

    private String normalizeOptionalText(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ServiceValidator.requireText(value, "externalSampleId", maximumLength);
    }

    private String hashRequest(HealthMetricBatchRequest request, String sourceType) {
        StringBuilder canonical = new StringBuilder(sourceType);
        if (request.items() != null) {
            request.items().forEach(item -> canonical.append('|').append(item));
        }
        return RequestHasher.sha256(canonical.toString());
    }

    private HealthMetricBatchResponse readStoredResponse(HealthMetricSyncRequestEntity entity) {
        try {
            return objectMapper.treeToValue(entity.getResponsePayload(), HealthMetricBatchResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored health synchronization response is invalid", exception);
        }
    }

    private record PreparedMetric(
            String metricType,
            BigDecimal value,
            String unit,
            OffsetDateTime recordedAt,
            String externalSampleId
    ) {
    }

    private static final class ValueAccumulator {
        private BigDecimal sum = BigDecimal.ZERO;
        private long count;

        void add(BigDecimal value) {
            sum = sum.add(value);
            count++;
        }

        BigDecimal average() {
            return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
    }
}
