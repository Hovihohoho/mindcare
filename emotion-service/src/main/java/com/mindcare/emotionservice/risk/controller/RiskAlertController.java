package com.mindcare.emotionservice.risk.controller;

import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import com.mindcare.emotionservice.risk.service.RiskService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/risk-alerts", produces = MediaType.APPLICATION_JSON_VALUE)
public class RiskAlertController {
    private final RiskService riskService;

    public RiskAlertController(RiskService riskService) { this.riskService = riskService; }

    @PostMapping("/analyze")
    public Optional<RiskAlertResponse> analyze(@AuthenticationPrincipal AuthenticatedUser user) {
        return riskService.analyzeRisk(user.userId());
    }

    @GetMapping
    public CursorPageResponse<RiskAlertResponse> history(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return riskService.getAlerts(user.userId(), from, to, cursor, limit);
    }

    @GetMapping("/{alertId}")
    public RiskAlertResponse detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID alertId) {
        return riskService.getAlert(user.userId(), alertId);
    }
}
