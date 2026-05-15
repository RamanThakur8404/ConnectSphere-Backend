package com.connectsphere.notification_service.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

// Base exception for all Notification Service exceptions.
public class NotificationServiceException extends RuntimeException {

	@Getter
    private final HttpStatus status;

    public NotificationServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public NotificationServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}