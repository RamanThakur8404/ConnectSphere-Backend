package com.connectsphere.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.connectsphere.payment_service.constant.Currency;
import com.connectsphere.payment_service.constant.PaymentStatus;
import com.connectsphere.payment_service.constant.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Public-safe representation of a {@link com.connectsphere.payment.entity.Payment}.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private Long userId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private Currency currency;
    private PaymentType paymentType;
    private PaymentStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
