package com.mindcare.bookingservice.shared.config;

import com.mindcare.bookingservice.integration.auth.ExpertProfileGateway;
import com.mindcare.bookingservice.integration.auth.ExpertDirectoryGateway;
import com.mindcare.bookingservice.payment.integration.PaymentProviderGateway;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ExternalGatewayFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(ExpertProfileGateway.class)
    ExpertProfileGateway unavailableExpertProfileGateway() {
        return expertUserId -> {
            throw new BusinessException(
                    "AUTH_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Expert profile verification is unavailable");
        };
    }

    @Bean
    @ConditionalOnMissingBean(ExpertDirectoryGateway.class)
    ExpertDirectoryGateway unavailableExpertDirectoryGateway() {
        return query -> {
            throw new BusinessException(
                    "AUTH_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Expert directory is unavailable");
        };
    }

    @Bean
    @ConditionalOnMissingBean(PaymentProviderGateway.class)
    PaymentProviderGateway unavailablePaymentProviderGateway() {
        return new PaymentProviderGateway() {
            @Override
            public com.mindcare.bookingservice.payment.integration.PaymentCheckoutResult
                    createCheckout(
                            com.mindcare.bookingservice.payment.integration.PaymentCheckoutCommand command) {
                throw unavailable();
            }

            @Override
            public String refund(
                    com.mindcare.bookingservice.payment.integration.PaymentRefundCommand command) {
                throw unavailable();
            }

            private BusinessException unavailable() {
                return new BusinessException(
                        "PAYMENT_PROVIDER_UNAVAILABLE",
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Payment provider is unavailable");
            }
        };
    }
}
