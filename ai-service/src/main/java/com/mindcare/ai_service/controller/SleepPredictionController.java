package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.dto.SleepPredictionRequest;
import com.mindcare.ai_service.dto.SleepPredictionResponse;
import com.mindcare.ai_service.service.SleepPredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/sleep")
@RequiredArgsConstructor
public class SleepPredictionController {
    private final SleepPredictionService service;

    @PostMapping("/predict")
    public ApiResponse<SleepPredictionResponse> predict(@Valid @RequestBody SleepPredictionRequest request) {
        return ApiResponse.success("Sleep duration prediction", service.predict(request));
    }
}
