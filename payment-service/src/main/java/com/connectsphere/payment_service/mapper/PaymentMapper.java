package com.connectsphere.payment_service.mapper;

import com.connectsphere.payment_service.dto.PaymentResponse;
import com.connectsphere.payment_service.entity.Payment;

// Maps Payment entity to response DTO.
public final class PaymentMapper {

    private PaymentMapper() {}

    // Convert entity to public-safe DTO.
    public static PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .userId(payment.getUserId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}