package com.mindcare.bookingservice.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking.policy")
public record BookingPolicyProperties(
        Duration slotHoldDuration,
        Duration automaticRefundCutoff,
        Duration cancellationClosedCutoff,
        Duration cancellationDecisionTimeout,
        Duration chatOpenBefore,
        Duration chatCloseAfter,
        Duration noShowGracePeriod,
        Duration completionReviewTimeout) {
}
