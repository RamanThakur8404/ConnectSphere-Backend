package com.connectsphere.notification_service.dto;

import java.time.LocalDateTime;

import com.connectsphere.notification_service.constants.NotificationTarget;
import com.connectsphere.notification_service.constants.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDTO {

	private Integer notificationId;
	private Integer recipientId;
	private Integer actorId;
	private NotificationType type;
	private String message;
	private Integer targetId;
	private NotificationTarget targetType;
	private String deepLinkUrl;
	private boolean isRead;
	private LocalDateTime createdAt;
}