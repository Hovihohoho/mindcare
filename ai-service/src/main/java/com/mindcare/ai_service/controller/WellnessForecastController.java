package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.dto.WellnessForecastRequest;
import com.mindcare.ai_service.dto.WellnessForecastResponse;
import com.mindcare.ai_service.service.WellnessModelRuntime;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/wellness-forecasts")
@ConditionalOnProperty(prefix = "ai.wellness-model", name = "enabled", havingValue = "true")
public class WellnessForecastController {
    private static final String USAGE_NOTICE =
            "Consumer-wearable wellness forecast; not a clinical diagnosis. Sleep and steps are estimates only.";
    private final WellnessModelRuntime runtime;

    public WellnessForecastController(WellnessModelRuntime runtime) {
        this.runtime = runtime;
    }

    @PostMapping
    ApiResponse<WellnessForecastResponse> predict(
            @Valid @RequestBody WellnessForecastRequest request
    ) {
        WellnessModelRuntime.Prediction prediction = runtime.predict(request.toFeatureArray());
        return ApiResponse.success("Next-day wellness indicators forecast", new WellnessForecastResponse(
                prediction.sleepMinutesNextDay(),
                prediction.stepsNextDay(),
                prediction.restingHeartRateNextDay(),
                prediction.modelVersion(),
                false,
                USAGE_NOTICE));
    }
}
