package com.connectsphere.payment_service.event;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Event published after a successful refund.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEvent {

    private Long paymentId;
    private Long userId;
    private String razorpayOrderId;
    private String razorpayRefundId;
    private BigDecimal refundedAmount;
    private String currency;
    private String refundNote;
}