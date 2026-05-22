package com.connectsphere.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;

// Thrown when a client attempts to mark a notification as read that has
@ResponseStatus(HttpStatus.CONFLICT)
public class NotificationAlreadyReadException extends NotificationServiceException {
	@Getter
    private final int notificationId;

    public NotificationAlreadyReadException(int notificationId) {
        super("Notification with ID " + notificationId + " is already marked as read.",
              HttpStatus.CONFLICT);
        this.notificationId = notificationId;
    }
}