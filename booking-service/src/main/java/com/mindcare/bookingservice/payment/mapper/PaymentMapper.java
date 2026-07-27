package com.mindcare.bookingservice.payment.mapper;

import com.mindcare.bookingservice.payment.dto.PaymentResponse;
import com.mindcare.bookingservice.payment.dto.RefundResponse;
import com.mindcare.bookingservice.payment.entity.Payment;
import com.mindcare.bookingservice.payment.entity.PaymentRefund;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

    RefundResponse toResponse(PaymentRefund refund);
}
