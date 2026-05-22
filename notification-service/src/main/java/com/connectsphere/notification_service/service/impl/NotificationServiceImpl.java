package com.connectsphere.notification_service.service.impl;

import com.connectsphere.notification_service.constants.ErrorMessages;
import com.connectsphere.notification_service.constants.LogMessages;
import com.connectsphere.notification_service.constants.NotificationTarget;
import com.connectsphere.notification_service.constants.NotificationType;
import com.connectsphere.notification_service.dto.CreateRequest;
import com.connectsphere.notification_service.dto.ResponseDTO;
import com.connectsphere.notification_service.dto.SummaryDTO;
import com.connectsphere.notification_service.entity.Notification;
import com.connectsphere.notification_service.exception.BulkNotificationException;
import com.connectsphere.notification_service.exception.EmailNotificationException;
import com.connectsphere.notification_service.exception.InvalidNotificationRequestException;
import com.connectsphere.notification_service.exception.NotificationNotFoundException;
import com.connectsphere.notification_service.exception.NotificationPersistenceException;
import com.connectsphere.notification_service.exception.NotificationServiceException;
import com.connectsphere.notification_service.mapper.NotificationMapper;
import com.connectsphere.notification_service.repository.NotificationRepository;
import com.connectsphere.notification_service.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.List;

// Service implementation for managing notifications.
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

	private final NotificationRepository notificationRepository;
	private final NotificationMapper notificationMapper;
	private final JavaMailSender mailSender;
	@Value("${spring.mail.username:no-reply@connectsphere.com}")
	private String mailFromAddress = "no-reply@connectsphere.com";

	// Creates a new notification.
	@Override
	public Notification createNotification(CreateRequest request) {
		log.info(LogMessages.CREATE_ATTEMPT, request.getRecipientId(), request.getType());
		validateCreateTarget(request);
		try {
			Notification entity = notificationMapper.toEntity(request);
			Notification saved = notificationRepository.save(entity);
			log.info(LogMessages.CREATE_SUCCESS, saved.getNotificationId());
			return saved;
		} catch (Exception ex) {
			log.error(LogMessages.CREATE_FAILED, request.getRecipientId());
			throw new NotificationPersistenceException(ErrorMessages.PERSIST_FAILED + request.getRecipientId(), ex);
		}
	}

	private void validateCreateTarget(CreateRequest request) {
		boolean hasTargetId = request.getTargetId() != null;
		boolean hasTargetType = request.getTargetType() != null;

		if (request.getType() == NotificationType.BROADCAST && (hasTargetId || hasTargetType)) {
			throw new InvalidNotificationRequestException(ErrorMessages.BROADCAST_TARGET_NOT_ALLOWED);
		}

		if (hasTargetId && !hasTargetType) {
			throw new InvalidNotificationRequestException(ErrorMessages.TARGET_ID_REQUIRES_TYPE);
		}

		if (hasTargetType && !hasTargetId) {
			throw new InvalidNotificationRequestException(ErrorMessages.TARGET_TYPE_REQUIRES_ID);
		}

		if (hasTargetType && !isValidTargetForType(request.getType(), request.getTargetType())) {
			throw new InvalidNotificationRequestException(ErrorMessages.INVALID_TARGET_FOR_TYPE + request.getType());
		}
	}

	private boolean isValidTargetForType(NotificationType type, NotificationTarget targetType) {
		if (type == null || targetType == null) {
			return true;
		}

		return switch (type) {
			case POST, LIKE -> targetType == NotificationTarget.POST;
			case COMMENT, REPLY -> targetType == NotificationTarget.COMMENT;
			case FOLLOW -> targetType == NotificationTarget.USER;
			case MESSAGE -> targetType == NotificationTarget.MESSAGE;
			case MENTION, REPORT_ACTION -> targetType == NotificationTarget.POST
					|| targetType == NotificationTarget.COMMENT
					|| targetType == NotificationTarget.USER;
			case BROADCAST -> false;
		};
	}

	// Sends a notification to multiple recipients.
	@Override
	public int sendBulkNotification(List<Integer> recipientIds, Integer actorId, NotificationType type, String message) {
		log.info(LogMessages.BULK_ATTEMPT, recipientIds != null ? recipientIds.size() : 0, message);

		if (recipientIds == null || recipientIds.isEmpty()) {
			log.warn(LogMessages.BULK_EMPTY_LIST);
			throw new BulkNotificationException(ErrorMessages.BULK_EMPTY_RECIPIENTS);
		}

		if (recipientIds.stream().anyMatch(recipientId -> recipientId == null || recipientId <= 0)) {
			throw new BulkNotificationException(ErrorMessages.BULK_INVALID_RECIPIENTS);
		}

		List<Integer> uniqueRecipientIds = recipientIds.stream().distinct().toList();

		if (uniqueRecipientIds.size() > 500) {
			log.warn(LogMessages.BULK_LIMIT_EXCEEDED, uniqueRecipientIds.size());
			throw new BulkNotificationException(ErrorMessages.BULK_LIMIT_EXCEEDED, uniqueRecipientIds.size());
		}

		if (actorId == null) {
			throw new BulkNotificationException("Actor ID must not be null.");
		}

		if (type == null) {
			throw new BulkNotificationException("Notification type must not be null.");
		}

		try {
			List<Notification> notifications = uniqueRecipientIds.stream().map(recipientId -> {
				Notification n = new Notification();
				n.setRecipientId(recipientId);
				n.setActorId(actorId);
				n.setType(type);
				n.setMessage(message);
				n.setRead(false);
				return n;
			}).toList();

			notificationRepository.saveAll(notifications);
			log.info(LogMessages.BULK_SUCCESS, notifications.size());
			return notifications.size();

		} catch (BulkNotificationException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error(LogMessages.BULK_FAILED, uniqueRecipientIds.size());
			throw new NotificationPersistenceException(
					ErrorMessages.PERSIST_BULK_FAILED + uniqueRecipientIds.size() + " recipients.", ex);
		}
	}

	// Marks a notification as read.
	@Override
	public void markAsRead(int notificationId) {
		log.info(LogMessages.MARK_READ_ATTEMPT, notificationId);
		Notification notification = findByIdOrThrow(notificationId);
		notification.setRead(true);
		try {
			notificationRepository.save(notification);
			log.info(LogMessages.MARK_READ_SUCCESS, notificationId);
		} catch (Exception ex) {
			throw new NotificationPersistenceException(ErrorMessages.PERSIST_MARK_READ + notificationId, ex);
		}
	}

	// Marks all unread notifications for a user as read.

	@Override
	public int markAllRead(int recipientId) {
		log.info(LogMessages.MARK_ALL_READ_ATTEMPT, recipientId);
		try {
			int updated = notificationRepository.markAllAsReadByRecipient(recipientId);
			log.info(LogMessages.MARK_ALL_READ_SUCCESS, recipientId, updated);
			return updated;
		} catch (Exception ex) {
			throw new NotificationPersistenceException(ErrorMessages.PERSIST_MARK_ALL_READ + recipientId, ex);
		}
	}

	// Retrieves all notifications for a user.
	@Override
	@Transactional(readOnly = true)
	public List<ResponseDTO> getByRecipient(int recipientId) {
		log.debug(LogMessages.GET_BY_RECIPIENT, recipientId);
		return notificationMapper
				.toResponseList(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId));
	}

	// Retrieves paginated notifications for a user.
	@Override
	@Transactional(readOnly = true)
	public List<SummaryDTO> getByRecipientPaged(int recipientId, int page, int size) {
		log.debug(LogMessages.GET_PAGED, recipientId, page, size);
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<Notification> result = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
		return notificationMapper.toSummaryList(result.getContent());
	}

	// Retrieves unread notifications for a user.
	@Override
	@Transactional(readOnly = true)
	public List<ResponseDTO> getUnreadByRecipient(int recipientId) {
		log.debug(LogMessages.GET_UNREAD, recipientId);
		return notificationMapper.toResponseList(notificationRepository.findByRecipientIdAndIsRead(recipientId, false));
	}

	// Returns unread notification count for a user. recipientId user ID unread
	@Override
	@Transactional(readOnly = true)
	public int getUnreadCount(int recipientId) {
		return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
	}

	// Retrieves all notifications (admin use case).
	@Override
	@Transactional(readOnly = true)
	public List<ResponseDTO> getAll() {
		log.debug(LogMessages.GET_ALL);
		return notificationMapper.toResponseList(notificationRepository.findAll());
	}

	// Deletes a notification by ID.
	@Override
	public void deleteNotification(int notificationId) {
		log.info(LogMessages.DELETE_ATTEMPT, notificationId);
		findByIdOrThrow(notificationId); // throws 404 if missing
		try {
			notificationRepository.deleteByNotificationId(notificationId);
			log.info(LogMessages.DELETE_SUCCESS, notificationId);
		} catch (Exception ex) {
			throw new NotificationPersistenceException(ErrorMessages.PERSIST_DELETE + notificationId, ex);
		}
	}

	// Sends an email alert.
	@Override
	public void sendEmailAlert(String toEmail, String subject, String body) {
		log.info(LogMessages.EMAIL_ATTEMPT, toEmail, subject);

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setFrom(mailFromAddress);
			helper.setText(toPlainText(body), toHtmlBody(subject, body));

			mailSender.send(message);
			log.info(LogMessages.EMAIL_SUCCESS, toEmail);
		} catch (MailException | MessagingException ex) {
			log.error(LogMessages.EMAIL_FAILED, toEmail);
			throw new EmailNotificationException(ErrorMessages.EMAIL_SEND_FAILED + toEmail, toEmail, ex);
		}
	}

	private String toHtmlBody(String subject, String body) {
		if (looksLikeHtml(body)) {
			return body;
		}

		String safeSubject = escapeHtml(subject);
		String formattedBody = escapeHtml(body).replace("\r\n", "\n").replace("\n", "<br>");

		return """
				<!DOCTYPE html>
				<html>
				  <body style="margin:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#172033;">
				    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f7fb;padding:32px 12px;">
				      <tr>
				        <td align="center">
				          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #dce4f2;border-radius:18px;overflow:hidden;">
				            <tr>
				              <td style="background:#111827;padding:26px 32px;color:#ffffff;">
				                <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;color:#93c5fd;">ConnectSphere</div>
				                <h1 style="margin:10px 0 0;font-size:24px;line-height:1.25;">%s</h1>
				              </td>
				            </tr>
				            <tr>
				              <td style="padding:30px 32px;font-size:16px;line-height:1.65;color:#253047;">%s</td>
				            </tr>
				            <tr>
				              <td style="padding:20px 32px;background:#f8fafc;color:#64748b;font-size:13px;line-height:1.5;">
				                You are receiving this email because of activity on your ConnectSphere account.
				              </td>
				            </tr>
				          </table>
				        </td>
				      </tr>
				    </table>
				  </body>
				</html>
				""".formatted(safeSubject, formattedBody);
	}

	private String toPlainText(String body) {
		if (!looksLikeHtml(body)) {
			return body;
		}

		return body.replaceAll("(?i)<br\\s*/?>", "\n")
				.replaceAll("(?i)</p>", "\n\n")
				.replaceAll("<[^>]+>", "")
				.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.trim();
	}

	private boolean looksLikeHtml(String value) {
		if (value == null) {
			return false;
		}
		String lower = value.toLowerCase();
		return lower.contains("<html") || lower.contains("<body") || lower.contains("<table")
				|| lower.contains("<!doctype");
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	// Fetches notification or throws exception if not found.
	private Notification findByIdOrThrow(int id) {
		return notificationRepository.findById(id).orElseThrow(() -> {
			log.warn(LogMessages.DELETE_NOT_FOUND, id);
			return new NotificationNotFoundException(id);
		});
	}
}
