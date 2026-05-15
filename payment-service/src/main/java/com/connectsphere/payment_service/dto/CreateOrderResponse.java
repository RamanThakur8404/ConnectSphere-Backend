package com.connectsphere.payment_service.dto;

import java.math.BigDecimal;

import com.connectsphere.payment_service.constant.Currency;
import com.connectsphere.payment_service.constant.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Response returned after a Razorpay order is created.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {

    // Internal ConnectSphere payment record ID. 
    private Long paymentId;

    // Razorpay Order ID (e.g., order_XXXXXXXXXX). Pass to checkout JS. 
    private String razorpayOrderId;

    // Razorpay Key ID (public key). Pass to checkout JS. 
    private String razorpayKeyId;

    // Amount in smallest currency unit (paise). 
    private BigDecimal amount;

    private Currency currency;
    private PaymentType paymentType;
    private String description;
}
