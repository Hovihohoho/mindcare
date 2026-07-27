package com.mindcare.bookingservice.payment.integration;

public interface PaymentProviderGateway {

    PaymentCheckoutResult createCheckout(PaymentCheckoutCommand command);

    String refund(PaymentRefundCommand command);
}
