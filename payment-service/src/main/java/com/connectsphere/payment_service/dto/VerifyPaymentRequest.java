package com.connectsphere.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Signed checkout payload returned by Razorpay after a successful browser payment.
@Data
public class VerifyPaymentRequest {

    @NotBlank(message = "Razorpay order id is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment id is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}
