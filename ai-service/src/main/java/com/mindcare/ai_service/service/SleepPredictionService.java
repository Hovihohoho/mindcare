package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.SleepPredictionRequest;
import com.mindcare.ai_service.dto.SleepPredictionResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SleepPredictionService {
    private final RestClient client;

    public SleepPredictionService(@Value("${sleep.inference-url:http://127.0.0.1:8085}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public SleepPredictionResponse predict(SleepPredictionRequest request) {
        if (request.date().equals(java.time.LocalDate.MAX)) {
            throw new IllegalArgumentException("Date outside supported range");
        }
        try {
            var result = client.post().uri("/predict").body(request).retrieve()
                    .body(SleepPredictionResponse.class);
            if (result == null || result.predictedSleepMinutes() == null
                    || !Double.isFinite(result.predictedSleepMinutes())
                    || result.predictedSleepMinutes() < 0 || result.predictedSleepMinutes() > 1440
                    || !request.date().equals(result.date())
                    || !request.date().plusDays(1).equals(result.targetDate())) {
                throw new IllegalStateException("Sleep inference returned an invalid response");
            }
            return result;
        } catch (RestClientException exception) {
            throw new IllegalStateException("Sleep prediction is unavailable. Please try again later.", exception);
        }
    }
}
