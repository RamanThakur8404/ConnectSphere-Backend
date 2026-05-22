package com.connectsphere.notification_service.constants;

// Centralised error / exception message strings for NotificationServiceImpl.
public final class ErrorMessages {

    private ErrorMessages() { /* constants-only class */ }

    // -----------------------------------------------------------------------
    // Notification not found
    // -----------------------------------------------------------------------
    public static final String NOTIFICATION_NOT_FOUND = "Notification not found with id: ";
    public static final String TARGET_ID_REQUIRES_TYPE = "Target ID requires a target type.";
    public static final String TARGET_TYPE_REQUIRES_ID = "Target type requires a target ID.";
    public static final String BROADCAST_TARGET_NOT_ALLOWED = "Broadcast notifications cannot reference a target.";
    public static final String INVALID_TARGET_FOR_TYPE = "Target type is not valid for notification type: ";

    // -----------------------------------------------------------------------
    // Bulk validation
    // -----------------------------------------------------------------------
    public static final String BULK_EMPTY_RECIPIENTS   = "Recipient list must not be empty.";
    public static final String BULK_INVALID_RECIPIENTS = "Recipient IDs must be positive numbers.";
    public static final String BULK_LIMIT_EXCEEDED     = "Bulk dispatch supports a maximum of 500 recipients per call.";

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------
    public static final String PERSIST_FAILED          = "Failed to persist notification for recipient ID: ";
    public static final String PERSIST_BULK_FAILED     = "Failed to persist bulk notifications for ";
    public static final String PERSIST_MARK_READ       = "Failed to mark notification as read. ID: ";
    public static final String PERSIST_MARK_ALL_READ   = "Failed to mark all notifications as read for recipient ID: ";
    public static final String PERSIST_DELETE          = "Failed to delete notification with ID: ";

    // -----------------------------------------------------------------------
    // Email
    // -----------------------------------------------------------------------
    public static final String EMAIL_SEND_FAILED       = "Failed to send email alert to: ";

    // -----------------------------------------------------------------------
    // Security / Authorization
    // -----------------------------------------------------------------------
    public static final String MISSING_USER_ID_HEADER  = "X-User-Id header is missing or invalid";
    public static final String ACCESS_DENIED           = "You do not have permission to perform this action";
}
