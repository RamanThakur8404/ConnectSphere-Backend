package com.connectsphere.payment_service.constant;

// Lifecycle states for a {@link com.connectsphere.payment.entity.Payment}.
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED
}
