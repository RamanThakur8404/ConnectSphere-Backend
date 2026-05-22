// ============================================================
package com.connectsphere.payment_service.dto;

import java.math.BigDecimal;

import com.connectsphere.payment_service.constant.Currency;
import com.connectsphere.payment_service.constant.PaymentType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Request body to create a Razorpay order.
@Data
public class CreateOrderRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1 (smallest currency unit)")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    // Optional human-readable note forwarded to Razorpay receipt field. 
    private String description;
}
