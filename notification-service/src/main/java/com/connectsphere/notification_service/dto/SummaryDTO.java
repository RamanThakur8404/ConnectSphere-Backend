package com.connectsphere.notification_service.dto;

import java.time.LocalDateTime;

import com.connectsphere.notification_service.constants.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryDTO {

	private Integer notificationId;
	private NotificationType type;
	private String message;
	private boolean isRead;
	private LocalDateTime createdAt;
	private String deepLinkUrl;
}
