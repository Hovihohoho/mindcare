package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.dto.StressPredictionRequest;
import com.mindcare.ai_service.dto.StressPredictionResponse;
import com.mindcare.ai_service.service.StressModelRuntime;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/stress-predictions")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.stress-model", name = "enabled", havingValue = "true")
public class StressPredictionController {
    private static final String USAGE_NOTICE =
            "Estimate of the PMData self-reported wellness score; not a clinical diagnosis.";

    private final StressModelRuntime modelRuntime;

    @PostMapping
    ApiResponse<StressPredictionResponse> predict(
            @Valid @RequestBody StressPredictionRequest request
    ) {
        StressModelRuntime.Prediction prediction =
                modelRuntime.predict(request.toFeatureArray());
        var response = new StressPredictionResponse(
                prediction.stressScore(),
                prediction.relativeLevel(),
                1,
                5,
                prediction.confidence(),
                prediction.modelVersion(),
                USAGE_NOTICE);
        return ApiResponse.success("Stress signal predicted", response);
    }
}
