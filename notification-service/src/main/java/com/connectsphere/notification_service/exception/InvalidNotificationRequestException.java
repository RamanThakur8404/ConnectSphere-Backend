package com.connectsphere.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a notification request contains invalid or incomplete data.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidNotificationRequestException extends NotificationServiceException {

    public InvalidNotificationRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public InvalidNotificationRequestException(String field, String reason) {
        super("Invalid value for field '" + field + "': " + reason, HttpStatus.BAD_REQUEST);
    }
}