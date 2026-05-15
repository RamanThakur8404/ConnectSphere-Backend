package com.connectsphere.message_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedMessageAccessException extends RuntimeException {
    public UnauthorizedMessageAccessException(String message) {
        super(message);
    }
}
