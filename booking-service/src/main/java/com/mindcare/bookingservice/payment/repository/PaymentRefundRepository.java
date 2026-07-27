package com.mindcare.bookingservice.payment.repository;

import com.mindcare.bookingservice.payment.entity.PaymentRefund;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {

    Optional<PaymentRefund> findByPaymentIdAndDeletedAtIsNull(UUID paymentId);
}
