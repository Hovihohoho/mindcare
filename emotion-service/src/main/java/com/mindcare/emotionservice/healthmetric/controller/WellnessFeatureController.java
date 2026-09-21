package com.mindcare.emotionservice.healthmetric.controller;

import com.mindcare.emotionservice.healthmetric.dto.WellnessFeatureResponse;
import com.mindcare.emotionservice.healthmetric.service.WellnessFeatureService;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/health-metrics", produces = MediaType.APPLICATION_JSON_VALUE)
public class WellnessFeatureController {
    private final WellnessFeatureService service;

    public WellnessFeatureController(WellnessFeatureService service) {
        this.service = service;
    }

    @GetMapping("/wellness-features")
    public WellnessFeatureResponse features(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone
    ) {
        try {
            return service.build(user.userId(), date, ZoneId.of(timezone));
        } catch (DateTimeException exception) {
            throw new InvalidRequestException("INVALID_TIMEZONE", "timezone must be a valid IANA zone");
        }
    }
}
