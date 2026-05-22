package com.connectsphere.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a notification cannot be saved, updated, or deleted due to a
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class NotificationPersistenceException extends NotificationServiceException {

    public NotificationPersistenceException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public NotificationPersistenceException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}