package com.connectsphere.notification_service.service;

import com.connectsphere.notification_service.constants.NotificationType;
import com.connectsphere.notification_service.dto.CreateRequest;
import com.connectsphere.notification_service.dto.ResponseDTO;
import com.connectsphere.notification_service.dto.SummaryDTO;
import com.connectsphere.notification_service.entity.Notification;

import java.util.List;

public interface NotificationService {

	//  Creation ────────────────────────────────────────────────────────────
	Notification createNotification(CreateRequest request);

	int sendBulkNotification(List<Integer> recipientIds, Integer actorId, NotificationType type, String message);

	void markAsRead(int notificationId);

	int markAllRead(int recipientId);

	List<ResponseDTO> getByRecipient(int recipientId);

	List<SummaryDTO> getByRecipientPaged(int recipientId, int page, int size);

	List<ResponseDTO> getUnreadByRecipient(int recipientId);

	int getUnreadCount(int recipientId);

	List<ResponseDTO> getAll();

	void deleteNotification(int notificationId);

	void sendEmailAlert(String toEmail, String subject, String body);
}
