package com.connectsphere.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;

// Thrown when sending an email alert fails.
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class EmailNotificationException extends NotificationServiceException {

	@Getter
    private final String toEmail;

    public EmailNotificationException(String message, String toEmail) {
        super(message, HttpStatus.BAD_GATEWAY);
        this.toEmail = toEmail;
    }

    public EmailNotificationException(String message, String toEmail, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, cause);
        this.toEmail = toEmail;
    }

    public EmailNotificationException(String message, String toEmail, HttpStatus status) {
        super(message, status);
        this.toEmail = toEmail;
    }
}