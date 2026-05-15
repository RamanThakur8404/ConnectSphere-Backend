package com.connectsphere.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;

// Thrown when a bulk notification dispatch fails.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BulkNotificationException extends NotificationServiceException {

	@Getter
	private final int recipientCount;

	public BulkNotificationException(String message) {
		super(message, HttpStatus.BAD_REQUEST);
		this.recipientCount = 0;
	}

	public BulkNotificationException(String message, int recipientCount) {
		super(message, HttpStatus.BAD_REQUEST);
		this.recipientCount = recipientCount;
	}

	public BulkNotificationException(String message, HttpStatus status, Throwable cause) {
		super(message, status, cause);
		this.recipientCount = 0;
	}
}