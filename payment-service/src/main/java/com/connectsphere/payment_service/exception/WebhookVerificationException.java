package com.connectsphere.payment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when Razorpay HMAC-SHA256 webhook signature verification fails. 
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class WebhookVerificationException extends RuntimeException {
    public WebhookVerificationException(String message) {
        super(message);
    }
}
