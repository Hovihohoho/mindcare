package com.mindcare.emotionservice.healthmetric.controller;

import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricTrendPointResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthSourceSummaryResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthBenchmarkEvaluation;
import com.mindcare.emotionservice.healthmetric.dto.DailyHealthEvaluationResponse;
import com.mindcare.emotionservice.healthmetric.service.HealthBenchmarkService;
import com.mindcare.emotionservice.healthmetric.service.HealthMetricService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/health-metrics", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthMetricController {
    private final HealthMetricService service;
    private final HealthBenchmarkService benchmarkService;

    public HealthMetricController(HealthMetricService service, HealthBenchmarkService benchmarkService) {
        this.service = service;
        this.benchmarkService = benchmarkService;
    }

    @PostMapping("/sync")
    public HealthMetricBatchResponse synchronize(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody HealthMetricBatchRequest request) {
        return service.synchronizeMetrics(user.userId(), idempotencyKey, request);
    }

    @GetMapping
    public CursorPageResponse<HealthMetricResponse> history(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String metricType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return service.getMetrics(user.userId(), metricType, from, to, cursor, limit);
    }

    @GetMapping("/trends")
    public List<HealthMetricTrendPointResponse> trends(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String metricType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "DAY") String bucket,
            @RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone) {
        return service.getMetricTrends(user.userId(), metricType, from, to, bucket, ZoneId.of(timezone));
    }

    @GetMapping("/benchmark-evaluations")
    public List<HealthBenchmarkEvaluation> benchmarkEvaluations(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return benchmarkService.evaluate(user.userId());
    }

    @GetMapping("/daily-evaluation")
    public DailyHealthEvaluationResponse dailyEvaluation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone
    ) {
        try {
            return benchmarkService.evaluateDay(user.userId(), date, ZoneId.of(timezone));
        } catch (DateTimeException exception) {
            throw new InvalidRequestException("INVALID_TIMEZONE", "timezone must be a valid IANA zone");
        }
    }

    @GetMapping("/sources/{sourceType}")
    public HealthSourceSummaryResponse sourceSummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @org.springframework.web.bind.annotation.PathVariable String sourceType) {
        return service.getSourceSummary(user.userId(), sourceType);
    }

    @PostMapping("/sources/{sourceType}/enable")
    public HealthSourceSummaryResponse enableSource(
            @AuthenticationPrincipal AuthenticatedUser user,
            @org.springframework.web.bind.annotation.PathVariable String sourceType) {
        return service.enableSourceSync(user.userId(), sourceType);
    }

    @DeleteMapping("/sources/{sourceType}/data")
    public HealthSourceSummaryResponse revokeAndDelete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @org.springframework.web.bind.annotation.PathVariable String sourceType) {
        return service.revokeAndDeleteSourceData(user.userId(), sourceType);
    }
}
