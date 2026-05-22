package com.connectsphere.notification_service.constants;

public class NotificationMessageConstants {
	public static final String MSG_NOTIFICATION_CREATED = "Notification created successfully.";
	public static final String MSG_NOTIFICATION_DELETED = "Notification deleted successfully.";
	public static final String MSG_MARKED_AS_READ = "Notification marked as read.";
	public static final String MSG_ALL_MARKED_AS_READ = "All notifications marked as read.";
	public static final String MSG_BULK_SENT = "Bulk notifications dispatched successfully.";
	public static final String MSG_EMAIL_ALERT_SENT = "Email alert dispatched.";

	public static final String ERR_NOTIFICATION_NOT_FOUND = "Notification not found with id: ";
	public static final String ERR_INVALID_TYPE = "Invalid notification type: ";
	public static final String ERR_RECIPIENT_REQUIRED = "Recipient ID must not be null.";
	public static final String ERR_ACTOR_REQUIRED = "Actor ID must not be null.";
	public static final String ERR_MESSAGE_BLANK = "Notification message must not be blank.";
	public static final String ERR_BULK_EMPTY = "Recipient list must not be empty for bulk dispatch.";
	public static final String ERR_EMAIL_SEND_FAILURE = "Failed to send email alert: ";
	public static final String ERR_UNAUTHORIZED_DELETE = "You are not authorised to delete this notification.";

}
