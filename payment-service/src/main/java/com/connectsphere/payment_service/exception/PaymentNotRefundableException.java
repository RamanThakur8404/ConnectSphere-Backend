package com.connectsphere.payment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a refund is attempted on a payment that is not in SUCCESS status. 
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentNotRefundableException extends RuntimeException {
    public PaymentNotRefundableException(String message) {
        super(message);
    }
}
