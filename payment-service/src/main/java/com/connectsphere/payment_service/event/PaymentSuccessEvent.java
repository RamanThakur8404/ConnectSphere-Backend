package com.connectsphere.payment_service.event;

import java.math.BigDecimal;

import com.connectsphere.payment_service.constant.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Event published on successful payment.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {

    private Long paymentId;
    private Long userId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private PaymentType paymentType;
}