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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HealthMetricServiceImpl implements HealthMetricService {
    private static final int MAX_BATCH_SIZE = 100;
    private static final Set<String> SOURCE_TYPES = Set.of("APPLE_HEALTH", "HEALTH_CONNECT", "GOOGLE_HEALTH", "MANUAL");
    private static final Set<String> METRIC_TYPES = Set.of("SLEEP_HOURS", "SLEEP_SESSION", "HEART_RATE", "STEP_COUNT", "EXERCISE_SESSION");

    private final HealthMetricRepository repository;
    private final HealthMetricSyncRequestRepository syncRequestRepository;
    private final HealthMetricMapper mapper;
    private final CursorCodec cursorCodec;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public HealthMetricServiceImpl(HealthMetricRepository repository, HealthMetricSyncRequestRepository syncRequestRepository,
                                   HealthMetricMapper mapper, CursorCodec cursorCodec, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.syncRequestRepository = syncRequestRepository;
        this.mapper = mapper;
        this.cursorCodec = cursorCodec;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public HealthMetricBatchResponse synchronizeMetrics(UUID userId, String idempotencyKey, HealthMetricBatchRequest request) {
        ServiceValidator.requireUserId(userId);
        String key = ServiceValidator.requireText(idempotencyKey, "idempotencyKey", 255);
        if (request == null) throw invalid("INVALID_REQUEST", "request must not be null");
        String sourceType = normalizeSource(request.sourceType());
        String requestHash = hashRequest(request, sourceType);
        var previous = syncRequestRepository.findByUserIdAndSourceTypeAndIdempotencyKey(userId, sourceType, key);
        if (previous.isPresent()) {
            if (!previous.get().getRequestHash().equals(requestHash)) {
                throw new ResourceConflictException("IDEMPOTENCY_CONFLICT", "idempotencyKey was already used with a different payload");
            }
            return storedResponse(previous.get());
        }
        if (request.items() == null || request.items().isEmpty() || request.items().size() > MAX_BATCH_SIZE) {
            throw invalid("INVALID_BATCH_SIZE", "items must contain between 1 and " + MAX_BATCH_SIZE + " entries");
        }
        List<PreparedMetric> prepared = request.items().stream().map(item -> prepare(item, sourceType)).toList();
        long externalIdCount = prepared.stream().map(PreparedMetric::externalId).filter(Objects::nonNull).count();
        if (prepared.stream().map(PreparedMetric::externalId).filter(Objects::nonNull).distinct().count() != externalIdCount) {
            throw invalid("DUPLICATE_EXTERNAL_SAMPLE_ID", "items must not repeat externalSampleId");
        }

        List<HealthMetricEntity> changed = new ArrayList<>();
        int accepted = 0;
        int updated = 0;
        int duplicates = 0;
        for (PreparedMetric metric : prepared) {
            var existing = repository.findByUserIdAndSourceTypeAndExternalSampleId(userId, sourceType, metric.externalId());
            if (existing.isEmpty()) {
                changed.add(new HealthMetricEntity(userId, metric.type(), metric.value(), metric.unit(), sourceType,
                        metric.externalId(), metric.recordedAt(), metric.startTime(), metric.endTime(), metric.sourceName(),
                        metric.dataOrigin(), metric.sourceModifiedAt(), metric.details()));
                accepted++;
            } else if (isUnchanged(existing.get(), metric)) {
                duplicates++;
            } else {
                existing.get().applyUpsert(metric.type(), metric.value(), metric.unit(), metric.recordedAt(), metric.startTime(),
                        metric.endTime(), metric.sourceName(), metric.dataOrigin(), metric.sourceModifiedAt(), metric.details());
                changed.add(existing.get());
                updated++;
            }
        }
        List<HealthMetricEntity> saved = repository.saveAllAndFlush(changed);
        HealthMetricBatchResponse response = new HealthMetricBatchResponse(accepted, updated, duplicates,
                saved.stream().map(HealthMetricEntity::getId).toList());
        syncRequestRepository.save(new HealthMetricSyncRequestEntity(userId, sourceType, key, requestHash, objectMapper.valueToTree(response)));
        return response;
    }

    @Override
    public CursorPageResponse<HealthMetricResponse> getMetrics(UUID userId, String metricType, OffsetDateTime from,
                                                               OffsetDateTime to, String cursor, int limit) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 365);
        ServiceValidator.validateLimit(limit);
        String type = normalizeMetricType(metricType);
        String scope = "health:" + userId + ':' + type + ':' + from + ':' + to;
        CursorCodec.CursorPosition position = cursorCodec.decode(cursor, scope);
        List<HealthMetricEntity> entities = repository.findHistory(userId, type, from, to, position.timestamp() != null,
                position.timestamp(), position.id(), PageRequest.of(0, limit + 1));
        boolean hasMore = entities.size() > limit;
        List<HealthMetricEntity> page = entities.subList(0, Math.min(limit, entities.size()));
        String next = hasMore ? cursorCodec.encode(page.get(page.size() - 1).getRecordedAt(), page.get(page.size() - 1).getId(), scope) : null;
        return new CursorPageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    @Override
    public List<HealthMetricTrendPointResponse> getMetricTrends(UUID userId, String metricType, OffsetDateTime from,
                                                                OffsetDateTime to, String bucket, ZoneId timezone) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 730);
        if (timezone == null) throw invalid("INVALID_TIMEZONE", "timezone must not be null");
        String type = normalizeMetricType(metricType);
        TrendBucket trendBucket = TrendBucket.parse(bucket);
        Map<OffsetDateTime, Values> groups = new LinkedHashMap<>();
        repository.findByUserIdAndMetricTypeAndDeletedAtIsNullAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
                userId, type, from, to).forEach(metric -> {
            BigDecimal value = aggregateValue(metric);
            if (value != null) groups.computeIfAbsent(trendBucket.startOf(metric.getRecordedAt(), timezone), ignored -> new Values()).add(value);
        });
        List<HealthMetricTrendPointResponse> result = new ArrayList<>();
        groups.forEach((start, values) -> result.add(new HealthMetricTrendPointResponse(start, trendBucket.endOf(start, timezone), type,
                "HEART_RATE".equals(type) ? values.average() : values.sum(), values.minimum, values.maximum, values.count,
                canonicalUnit(type), "HEART_RATE".equals(type) ? "AVERAGE" : "SUM")));
        return List.copyOf(result);
    }

    private PreparedMetric prepare(HealthMetricItemRequest item, String sourceType) {
        if (item == null) throw invalid("INVALID_HEALTH_METRIC", "item must not be null");
        String externalId = text(item.externalSampleId(), 255);
        if (!"MANUAL".equals(sourceType) && externalId == null) throw invalid("EXTERNAL_SAMPLE_ID_REQUIRED", "externalSampleId is required");
        String type = normalizeMetricType(item.metricType());
        OffsetDateTime recordedAt = item.recordedAt() != null ? item.recordedAt() : item.endTime();
        validateTime(recordedAt, "recordedAt");
        if (item.sourceLastModifiedAt() != null && item.sourceLastModifiedAt().isAfter(OffsetDateTime.now(clock).plusMinutes(5))) {
            throw invalid("INVALID_SOURCE_MODIFIED_AT", "sourceLastModifiedAt is in the future");
        }
        Map<String, Object> details = item.details() == null ? Map.of() : item.details();
        try {
            if (objectMapper.writeValueAsString(details).length() > 20_000) throw invalid("HEALTH_DETAILS_TOO_LARGE", "details exceeds maximum size");
        } catch (JsonProcessingException exception) {
            throw invalid("INVALID_HEALTH_DETAILS", "details is not valid JSON");
        }
        if (Set.of("SLEEP_SESSION", "EXERCISE_SESSION").contains(type)) {
            if (item.startTime() == null || item.endTime() == null || !item.startTime().isBefore(item.endTime())) {
                throw invalid("INVALID_SESSION_RANGE", "session requires startTime before endTime");
            }
            validateTime(item.endTime(), "endTime");
            return new PreparedMetric(externalId, type, null, null, recordedAt, item.startTime(), item.endTime(), text(item.sourceName(), 255),
                    text(item.dataOrigin(), 255), item.sourceLastModifiedAt(), details);
        }
        Canonical canonical = canonicalize(type, item.value(), item.unit());
        return new PreparedMetric(externalId, type, canonical.value(), canonical.unit(), recordedAt, item.startTime(), item.endTime(),
                text(item.sourceName(), 255), text(item.dataOrigin(), 255), item.sourceLastModifiedAt(), details);
    }

    private Canonical canonicalize(String type, BigDecimal value, String unit) {
        if (value == null) throw invalid("INVALID_HEALTH_METRIC", "value is required for " + type);
        String normalizedUnit = ServiceValidator.requireText(unit, "unit", 20).toLowerCase(Locale.ROOT);
        if ("SLEEP_HOURS".equals(type)) {
            BigDecimal hours = Set.of("h", "hour", "hours").contains(normalizedUnit) ? value
                    : Set.of("min", "minute", "minutes").contains(normalizedUnit) ? value.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                    : null;
            if (hours == null) throw invalid("INVALID_METRIC_UNIT", "unit is not supported for " + type);
            range(hours, BigDecimal.ZERO, BigDecimal.valueOf(24), type);
            return new Canonical(hours, "h");
        }
        if ("HEART_RATE".equals(type)) {
            if (!"bpm".equals(normalizedUnit)) throw invalid("INVALID_METRIC_UNIT", "unit is not supported for " + type);
            range(value, BigDecimal.valueOf(20), BigDecimal.valueOf(250), type);
            return new Canonical(value, "bpm");
        }
        if (!Set.of("count", "step", "steps").contains(normalizedUnit) || value.stripTrailingZeros().scale() > 0) {
            throw invalid("INVALID_METRIC_VALUE", "STEP_COUNT requires an integer count");
        }
        range(value, BigDecimal.ZERO, BigDecimal.valueOf(200_000), type);
        return new Canonical(value, "count");
    }

    private boolean isUnchanged(HealthMetricEntity entity, PreparedMetric metric) {
        if (metric.sourceModifiedAt() != null && entity.getSourceLastModifiedAt() != null) {
            return !metric.sourceModifiedAt().isAfter(entity.getSourceLastModifiedAt());
        }
        return entity.getDeletedAt() == null && Objects.equals(entity.getMetricType(), metric.type())
                && Objects.equals(entity.getMetricValue(), metric.value()) && Objects.equals(entity.getUnit(), metric.unit())
                && Objects.equals(entity.getRecordedAt(), metric.recordedAt()) && Objects.equals(entity.getStartTime(), metric.startTime())
                && Objects.equals(entity.getEndTime(), metric.endTime()) && Objects.equals(entity.getDetails(), metric.details());
    }

    private BigDecimal aggregateValue(HealthMetricEntity metric) {
        if (Set.of("SLEEP_SESSION", "EXERCISE_SESSION").contains(metric.getMetricType())) {
            if (metric.getStartTime() == null || metric.getEndTime() == null) return null;
            return BigDecimal.valueOf(Duration.between(metric.getStartTime(), metric.getEndTime()).toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
        return metric.getMetricValue();
    }

    private void validateTime(OffsetDateTime value, String field) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (value == null || value.isAfter(now.plusMinutes(5)) || value.isBefore(now.minusDays(30))) {
            throw invalid("INVALID_RECORDED_AT", field + " must be within the accepted 30-day synchronization window");
        }
    }

    private String normalizeSource(String source) {
        String value = ServiceValidator.requireText(source, "sourceType", 50).toUpperCase(Locale.ROOT);
        if ("GOOGLE_HEALTH".equals(value)) value = "HEALTH_CONNECT";
        if (!SOURCE_TYPES.contains(value)) throw invalid("INVALID_SOURCE_TYPE", "unsupported sourceType");
        return value;
    }

    private String normalizeMetricType(String type) {
        String value = ServiceValidator.requireText(type, "metricType", 50).toUpperCase(Locale.ROOT);
        if (!METRIC_TYPES.contains(value)) throw invalid("INVALID_METRIC_TYPE", "unsupported metricType");
        return value;
    }

    private String canonicalUnit(String type) {
        return switch (type) {
            case "HEART_RATE" -> "bpm";
            case "STEP_COUNT" -> "count";
            default -> "h";
        };
    }

    private void range(BigDecimal value, BigDecimal min, BigDecimal max, String type) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) throw invalid("INVALID_METRIC_VALUE", type + " is outside ingest range");
    }

    private String text(String value, int max) {
        return value == null || value.isBlank() ? null : ServiceValidator.requireText(value, "text", max);
    }

    private String hashRequest(HealthMetricBatchRequest request, String source) {
        try {
            return RequestHasher.sha256(source + '|' + objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException exception) {
            throw invalid("INVALID_REQUEST", "request cannot be serialized");
        }
    }

    private HealthMetricBatchResponse storedResponse(HealthMetricSyncRequestEntity entity) {
        try {
            return objectMapper.treeToValue(entity.getResponsePayload(), HealthMetricBatchResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored health synchronization response is invalid", exception);
        }
    }

    private InvalidRequestException invalid(String code, String message) { return new InvalidRequestException(code, message); }
    private record Canonical(BigDecimal value, String unit) {}
    private record PreparedMetric(String externalId, String type, BigDecimal value, String unit, OffsetDateTime recordedAt,
                                  OffsetDateTime startTime, OffsetDateTime endTime, String sourceName, String dataOrigin,
                                  OffsetDateTime sourceModifiedAt, Map<String, Object> details) {}

    private static final class Values {
        private BigDecimal sum = BigDecimal.ZERO;
        private BigDecimal minimum;
        private BigDecimal maximum;
        private long count;
        void add(BigDecimal value) {
            sum = sum.add(value);
            minimum = minimum == null || value.compareTo(minimum) < 0 ? value : minimum;
            maximum = maximum == null || value.compareTo(maximum) > 0 ? value : maximum;
            count++;
        }
        BigDecimal sum() { return sum.setScale(2, RoundingMode.HALF_UP); }
        BigDecimal average() { return count == 0 ? null : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP); }
    }
}
