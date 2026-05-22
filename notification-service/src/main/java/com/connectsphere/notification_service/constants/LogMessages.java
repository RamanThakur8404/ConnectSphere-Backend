package com.connectsphere.notification_service.constants;

// Centralised log message templates for NotificationServiceImpl.
public final class LogMessages {

    private LogMessages() { /* constants-only class — no instantiation */ }

    // -----------------------------------------------------------------------
    // Notification Creation
    // -----------------------------------------------------------------------
    public static final String CREATE_ATTEMPT        = "Creating notification — recipientId: {}, type: {}";
    public static final String CREATE_SUCCESS        = "Notification created successfully — notificationId: {}";
    public static final String CREATE_FAILED         = "Failed to persist notification — recipientId: {}";

    // -----------------------------------------------------------------------
    // Bulk Notification
    // -----------------------------------------------------------------------
    public static final String BULK_ATTEMPT          = "Sending bulk notification — recipientCount: {}, message: {}";
    public static final String BULK_SUCCESS          = "Bulk notifications dispatched — count: {}";
    public static final String BULK_FAILED           = "Bulk notification failed — recipientCount: {}";
    public static final String BULK_EMPTY_LIST       = "Bulk notification rejected — empty recipient list";
    public static final String BULK_LIMIT_EXCEEDED   = "Bulk notification rejected — limit exceeded: {} recipients";

    // -----------------------------------------------------------------------
    // Mark as Read
    // -----------------------------------------------------------------------
    public static final String MARK_READ_ATTEMPT     = "Marking notification as read — notificationId: {}";
    public static final String MARK_READ_SUCCESS     = "Notification marked as read — notificationId: {}";
    public static final String MARK_ALL_READ_ATTEMPT = "Marking all notifications as read — recipientId: {}";
    public static final String MARK_ALL_READ_SUCCESS = "All notifications marked as read — recipientId: {}, count: {}";

    // -----------------------------------------------------------------------
    // Retrieval
    // -----------------------------------------------------------------------
    public static final String GET_BY_RECIPIENT      = "Fetching notifications — recipientId: {}";
    public static final String GET_PAGED             = "Fetching paged notifications — recipientId: {}, page: {}, size: {}";
    public static final String GET_UNREAD            = "Fetching unread notifications — recipientId: {}";
    public static final String GET_UNREAD_COUNT      = "Fetching unread count — recipientId: {}";
    public static final String GET_ALL               = "Fetching all notifications (admin request)";

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------
    public static final String DELETE_ATTEMPT        = "Deleting notification — notificationId: {}";
    public static final String DELETE_SUCCESS        = "Notification deleted — notificationId: {}";
    public static final String DELETE_NOT_FOUND      = "Delete failed — notification not found: {}";

    // -----------------------------------------------------------------------
    // Email Alert
    // -----------------------------------------------------------------------
    public static final String EMAIL_ATTEMPT         = "Sending email alert — to: {}, subject: {}";
    public static final String EMAIL_SUCCESS         = "Email alert sent successfully — to: {}";
    public static final String EMAIL_FAILED          = "Email alert failed — to: {}";

    // -----------------------------------------------------------------------
    // Security (header-based auth)
    // -----------------------------------------------------------------------
    public static final String HEADER_AUTH_SUCCESS   = "Request authenticated via X-User-Id header — userId: {}, role: {}";
    public static final String HEADER_AUTH_MISSING   = "Request missing X-User-Id header — path: {}";
}
