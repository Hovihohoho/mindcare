package com.mindcare.bookingservice.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking.payment")
public record BookingPaymentProperties(boolean required) {
}
