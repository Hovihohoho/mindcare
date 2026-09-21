package com.mindcare.ai_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.mindcare.ai_service.config.GeminiProperties;
import com.mindcare.ai_service.config.StressModelProperties;
import com.mindcare.ai_service.config.WellnessModelProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({GeminiProperties.class, StressModelProperties.class, WellnessModelProperties.class})
@EnableScheduling
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
