package com.connectsphere.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a notification cannot be found by its ID.
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends NotificationServiceException {

    public NotificationNotFoundException(int notificationId) {
        super("Notification not found with ID: " + notificationId, HttpStatus.NOT_FOUND);
    }

    public NotificationNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}