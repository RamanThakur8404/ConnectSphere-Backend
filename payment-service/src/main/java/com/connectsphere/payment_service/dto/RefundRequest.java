package com.connectsphere.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

// Admin-only request to issue a refund for a completed payment.
@Data
public class RefundRequest {

    // Amount to refund in the smallest currency unit (paise).
    @DecimalMin(value = "1.0", message = "Refund amount must be at least 1")
    private BigDecimal refundAmount;

    // Admin note explaining the reason for the refund. Stored in the payment record. 
    @NotBlank(message = "Refund note is required")
    private String refundNote;
}
