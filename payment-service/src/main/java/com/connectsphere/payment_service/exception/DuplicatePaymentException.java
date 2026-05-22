package com.connectsphere.payment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a duplicate idempotency key is detected for a payment order. 
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
