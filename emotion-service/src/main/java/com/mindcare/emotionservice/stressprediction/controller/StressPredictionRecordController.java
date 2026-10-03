package com.mindcare.emotionservice.stressprediction.controller;

import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import com.mindcare.emotionservice.stressprediction.dto.StoreStressPredictionRequest;
import com.mindcare.emotionservice.stressprediction.dto.StressPredictionRecordResponse;
import com.mindcare.emotionservice.stressprediction.service.StressPredictionRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/health-metrics/stress-predictions")
@RequiredArgsConstructor
public class StressPredictionRecordController {
    private final StressPredictionRecordService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StressPredictionRecordResponse store(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody StoreStressPredictionRequest request
    ) {
        return service.store(user.userId(), request);
    }

    @GetMapping("/latest")
    public Optional<StressPredictionRecordResponse> latest(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.latest(user.userId());
    }
}
