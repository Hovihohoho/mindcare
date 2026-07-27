package com.mindcare.bookingservice.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        BookingPolicyProperties.class,
        BookingPaymentProperties.class
})
public class PolicyConfig {
}
