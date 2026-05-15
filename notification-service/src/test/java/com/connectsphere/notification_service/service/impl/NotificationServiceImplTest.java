package com.connectsphere.notification_service.service.impl;

import com.connectsphere.notification_service.constants.NotificationType;
import com.connectsphere.notification_service.constants.NotificationTarget;
import com.connectsphere.notification_service.dto.CreateRequest;
import com.connectsphere.notification_service.dto.ResponseDTO;
import com.connectsphere.notification_service.dto.SummaryDTO;
import com.connectsphere.notification_service.entity.Notification;
import com.connectsphere.notification_service.exception.BulkNotificationException;
import com.connectsphere.notification_service.exception.EmailNotificationException;
import com.connectsphere.notification_service.exception.InvalidNotificationRequestException;
import com.connectsphere.notification_service.exception.NotificationNotFoundException;
import com.connectsphere.notification_service.exception.NotificationPersistenceException;
import com.connectsphere.notification_service.mapper.NotificationMapper;
import com.connectsphere.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private NotificationMapper notificationMapper;

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	// -----------------------------------------------------------------------
	// Test fixtures
	// -----------------------------------------------------------------------

	private Notification sampleNotification;
	private CreateRequest sampleCreateRequest;
	private ResponseDTO sampleResponseDTO;

	@BeforeEach
	void setUp() {
		sampleNotification = new Notification();
		sampleNotification.setNotificationId(1);
		sampleNotification.setRecipientId(101);
		sampleNotification.setActorId(202);
		sampleNotification.setType(NotificationType.LIKE);
		sampleNotification.setMessage("Alice liked your post.");
		sampleNotification.setRead(false);
		sampleNotification.setCreatedAt(LocalDateTime.now());

		sampleCreateRequest = new CreateRequest();
		sampleCreateRequest.setRecipientId(101);
		sampleCreateRequest.setActorId(202);
		sampleCreateRequest.setType(NotificationType.LIKE);
		sampleCreateRequest.setMessage("Alice liked your post.");

		sampleResponseDTO = ResponseDTO.builder().notificationId(1).recipientId(101).actorId(202)
				.type(NotificationType.LIKE).message("Alice liked your post.").isRead(false)
				.createdAt(LocalDateTime.now()).build();

		ReflectionTestUtils.setField(notificationService, "mailFromAddress", "no-reply@connectsphere.com");
		lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
	}

	// ===================================================================
	// createNotification
	// ===================================================================

	@Nested
	@DisplayName("createNotification()")
	class CreateNotificationTests {

		@Test
		@DisplayName("createNotification_ValidRequest_ReturnsPersistedEntity")
		void createNotification_ValidRequest_ReturnsPersistedEntity() {
			when(notificationMapper.toEntity(sampleCreateRequest)).thenReturn(sampleNotification);
			when(notificationRepository.save(sampleNotification)).thenReturn(sampleNotification);

			Notification result = notificationService.createNotification(sampleCreateRequest);

			assertThat(result).isNotNull();
			assertThat(result.getNotificationId()).isEqualTo(1);
			assertThat(result.getRecipientId()).isEqualTo(101);
			verify(notificationRepository, times(1)).save(sampleNotification);
		}

		@Test
		@DisplayName("createNotification_RepositoryThrows_WrapsInPersistenceException")
		void createNotification_RepositoryThrows_WrapsInPersistenceException() {
			when(notificationMapper.toEntity(sampleCreateRequest)).thenReturn(sampleNotification);
			when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("DB error"));

			assertThatThrownBy(() -> notificationService.createNotification(sampleCreateRequest))
					.isInstanceOf(NotificationPersistenceException.class).hasMessageContaining("101");
		}

		@Test
		@DisplayName("createNotification_BroadcastWithTarget_ThrowsInvalidNotificationRequestException")
		void createNotification_BroadcastWithTarget_ThrowsInvalidNotificationRequestException() {
			sampleCreateRequest.setType(NotificationType.BROADCAST);
			sampleCreateRequest.setTargetType(NotificationTarget.POST);
			sampleCreateRequest.setTargetId(99);

			assertThatThrownBy(() -> notificationService.createNotification(sampleCreateRequest))
					.isInstanceOf(InvalidNotificationRequestException.class)
					.hasMessageContaining("Broadcast");
		}

		@Test
		@DisplayName("createNotification_TargetTypeWithoutTargetId_ThrowsInvalidNotificationRequestException")
		void createNotification_TargetTypeWithoutTargetId_ThrowsInvalidNotificationRequestException() {
			sampleCreateRequest.setTargetType(NotificationTarget.POST);

			assertThatThrownBy(() -> notificationService.createNotification(sampleCreateRequest))
					.isInstanceOf(InvalidNotificationRequestException.class)
					.hasMessageContaining("Target type");
		}

		@Test
		@DisplayName("createNotification_InvalidTargetForType_ThrowsInvalidNotificationRequestException")
		void createNotification_InvalidTargetForType_ThrowsInvalidNotificationRequestException() {
			sampleCreateRequest.setType(NotificationType.FOLLOW);
			sampleCreateRequest.setTargetType(NotificationTarget.POST);
			sampleCreateRequest.setTargetId(99);

			assertThatThrownBy(() -> notificationService.createNotification(sampleCreateRequest))
					.isInstanceOf(InvalidNotificationRequestException.class)
					.hasMessageContaining("FOLLOW");
		}
	}

	// ===================================================================
	// sendBulkNotification
	// ===================================================================

	@Nested
	@DisplayName("sendBulkNotification()")
	class SendBulkNotificationTests {

		@Test
		@DisplayName("sendBulkNotification_ValidList_SavesAll")
		void sendBulkNotification_ValidList_SavesAll() {
			List<Integer> recipientIds = Arrays.asList(1, 2, 3);
			int createdCount = notificationService.sendBulkNotification(recipientIds, 303, NotificationType.BROADCAST, "Platform update");

			assertThat(createdCount).isEqualTo(3);
			verify(notificationRepository, times(1)).saveAll(anyList());
		}

		@Test
		@DisplayName("sendBulkNotification_DuplicateRecipients_SavesUniqueRecipients")
		void sendBulkNotification_DuplicateRecipients_SavesUniqueRecipients() {
			int createdCount = notificationService.sendBulkNotification(Arrays.asList(1, 2, 2, 3, 1), 303, NotificationType.BROADCAST, "Platform update");

			assertThat(createdCount).isEqualTo(3);
			verify(notificationRepository).saveAll(argThat(notifications -> {
				int count = 0;
				for (Notification ignored : notifications) {
					count++;
				}
				return count == 3;
			}));
		}

		@Test
		@DisplayName("sendBulkNotification_EmptyList_ThrowsBulkNotificationException")
		void sendBulkNotification_EmptyList_ThrowsBulkNotificationException() {
			assertThatThrownBy(() -> notificationService.sendBulkNotification(Collections.emptyList(), 303, NotificationType.BROADCAST, "msg"))
					.isInstanceOf(BulkNotificationException.class).hasMessageContaining("empty");
		}

		@Test
		@DisplayName("sendBulkNotification_NullList_ThrowsBulkNotificationException")
		void sendBulkNotification_NullList_ThrowsBulkNotificationException() {
			assertThatThrownBy(() -> notificationService.sendBulkNotification(null, 303, NotificationType.BROADCAST, "msg"))
					.isInstanceOf(BulkNotificationException.class);
		}

		@Test
		@DisplayName("sendBulkNotification_Over500Recipients_ThrowsBulkNotificationException")
		void sendBulkNotification_Over500Recipients_ThrowsBulkNotificationException() {
			List<Integer> tooMany = IntStream.rangeClosed(1, 501).boxed().toList();
			assertThatThrownBy(() -> notificationService.sendBulkNotification(tooMany, 303, NotificationType.BROADCAST, "msg"))
					.isInstanceOf(BulkNotificationException.class).hasMessageContaining("500");
		}

		@Test
		@DisplayName("sendBulkNotification_InvalidRecipient_ThrowsBulkNotificationException")
		void sendBulkNotification_InvalidRecipient_ThrowsBulkNotificationException() {
			assertThatThrownBy(() -> notificationService.sendBulkNotification(Arrays.asList(1, 0), 303, NotificationType.BROADCAST, "msg"))
					.isInstanceOf(BulkNotificationException.class)
					.hasMessageContaining("positive");
		}

		@Test
		@DisplayName("sendBulkNotification_NullActorId_ThrowsBulkNotificationException")
		void sendBulkNotification_NullActorId_ThrowsBulkNotificationException() {
			assertThatThrownBy(() -> notificationService.sendBulkNotification(Arrays.asList(1, 2), null, NotificationType.BROADCAST, "msg"))
					.isInstanceOf(BulkNotificationException.class)
					.hasMessageContaining("Actor ID");
		}

		@Test
		@DisplayName("sendBulkNotification_NullType_ThrowsBulkNotificationException")
		void sendBulkNotification_NullType_ThrowsBulkNotificationException() {
			assertThatThrownBy(() -> notificationService.sendBulkNotification(Arrays.asList(1, 2), 303, null, "msg"))
					.isInstanceOf(BulkNotificationException.class)
					.hasMessageContaining("Notification type");
		}

		@Test
		@DisplayName("sendBulkNotification_RepositoryThrows_WrapsInPersistenceException")
		void sendBulkNotification_RepositoryThrows_WrapsInPersistenceException() {
			when(notificationRepository.saveAll(anyList())).thenThrow(new RuntimeException("DB down"));

			assertThatThrownBy(() -> notificationService.sendBulkNotification(Arrays.asList(1, 2), 303, NotificationType.BROADCAST, "msg"))
					.isInstanceOf(NotificationPersistenceException.class);
		}
	}

	// ===================================================================
	// markAsRead
	// ===================================================================

	@Nested
	@DisplayName("markAsRead()")
	class MarkAsReadTests {

		@Test
		@DisplayName("markAsRead_ExistingId_SetsReadTrue")
		void markAsRead_ExistingId_SetsReadTrue() {
			when(notificationRepository.findById(1)).thenReturn(Optional.of(sampleNotification));

			notificationService.markAsRead(1);

			assertThat(sampleNotification.isRead()).isTrue();
			verify(notificationRepository, times(1)).save(sampleNotification);
		}

		@Test
		@DisplayName("markAsRead_NotFound_ThrowsNotificationNotFoundException")
		void markAsRead_NotFound_ThrowsNotificationNotFoundException() {
			when(notificationRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> notificationService.markAsRead(999))
					.isInstanceOf(NotificationNotFoundException.class);
		}

		@Test
		@DisplayName("markAsRead_SaveThrows_WrapsInPersistenceException")
		void markAsRead_SaveThrows_WrapsInPersistenceException() {
			when(notificationRepository.findById(1)).thenReturn(Optional.of(sampleNotification));
			when(notificationRepository.save(any())).thenThrow(new RuntimeException("DB error"));

			assertThatThrownBy(() -> notificationService.markAsRead(1))
					.isInstanceOf(NotificationPersistenceException.class).hasMessageContaining("1");
		}
	}

	// ===================================================================
	// markAllRead
	// ===================================================================

	@Nested
	@DisplayName("markAllRead()")
	class MarkAllReadTests {

		@Test
		@DisplayName("markAllRead_ValidRecipient_ReturnsUpdatedCount")
		void markAllRead_ValidRecipient_ReturnsUpdatedCount() {
			when(notificationRepository.markAllAsReadByRecipient(101)).thenReturn(3);

			int count = notificationService.markAllRead(101);

			assertThat(count).isEqualTo(3);
			verify(notificationRepository).markAllAsReadByRecipient(101);
		}

		@Test
		@DisplayName("markAllRead_RepositoryThrows_WrapsInPersistenceException")
		void markAllRead_RepositoryThrows_WrapsInPersistenceException() {
			when(notificationRepository.markAllAsReadByRecipient(anyInt())).thenThrow(new RuntimeException("DB error"));

			assertThatThrownBy(() -> notificationService.markAllRead(101))
					.isInstanceOf(NotificationPersistenceException.class);
		}
	}

	// ===================================================================
	// getByRecipient
	// ===================================================================

	@Nested
	@DisplayName("getByRecipient()")
	class GetByRecipientTests {

		@Test
		@DisplayName("getByRecipient_ExistingRecipient_ReturnsMappedDTOs")
		void getByRecipient_ExistingRecipient_ReturnsMappedDTOs() {
			when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(101))
					.thenReturn(List.of(sampleNotification));
			when(notificationMapper.toResponseList(anyList())).thenReturn(List.of(sampleResponseDTO));

			List<ResponseDTO> result = notificationService.getByRecipient(101);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).getNotificationId()).isEqualTo(1);
		}

		@Test
		@DisplayName("getByRecipient_NoNotifications_ReturnsEmptyList")
		void getByRecipient_NoNotifications_ReturnsEmptyList() {
			when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(999)).thenReturn(Collections.emptyList());
			when(notificationMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

			List<ResponseDTO> result = notificationService.getByRecipient(999);

			assertThat(result).isEmpty();
		}
	}

	// ===================================================================
	// getByRecipientPaged
	// ===================================================================

	@Nested
	@DisplayName("getByRecipientPaged()")
	class GetByRecipientPagedTests {

		@Test
		@DisplayName("getByRecipientPaged_ValidParams_ReturnsSummaryDTOs")
		void getByRecipientPaged_ValidParams_ReturnsSummaryDTOs() {
			when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(101), any(Pageable.class)))
					.thenReturn(new PageImpl<>(List.of(sampleNotification)));
			when(notificationMapper.toSummaryList(anyList())).thenReturn(List.of(new SummaryDTO()));

			List<SummaryDTO> result = notificationService.getByRecipientPaged(101, 0, 20);

			assertThat(result).hasSize(1);
		}
	}

	// ===================================================================
	// getUnreadByRecipient
	// ===================================================================

	@Nested
	@DisplayName("getUnreadByRecipient()")
	class GetUnreadTests {

		@Test
		@DisplayName("getUnreadByRecipient_HasUnread_ReturnsList")
		void getUnreadByRecipient_HasUnread_ReturnsList() {
			when(notificationRepository.findByRecipientIdAndIsRead(101, false)).thenReturn(List.of(sampleNotification));
			when(notificationMapper.toResponseList(anyList())).thenReturn(List.of(sampleResponseDTO));

			List<ResponseDTO> result = notificationService.getUnreadByRecipient(101);

			assertThat(result).hasSize(1);
		}
	}

	// ===================================================================
	// getUnreadCount
	// ===================================================================

	@Nested
	@DisplayName("getUnreadCount()")
	class GetUnreadCountTests {

		@Test
		@DisplayName("getUnreadCount_ValidRecipient_ReturnsCount")
		void getUnreadCount_ValidRecipient_ReturnsCount() {
			when(notificationRepository.countByRecipientIdAndIsRead(101, false)).thenReturn(7);

			int count = notificationService.getUnreadCount(101);

			assertThat(count).isEqualTo(7);
		}

		@Test
		@DisplayName("getUnreadCount_NoUnread_ReturnsZero")
		void getUnreadCount_NoUnread_ReturnsZero() {
			when(notificationRepository.countByRecipientIdAndIsRead(101, false)).thenReturn(0);

			int count = notificationService.getUnreadCount(101);

			assertThat(count).isZero();
		}
	}

	// ===================================================================
	// getAll
	// ===================================================================

	@Nested
	@DisplayName("getAll()")
	class GetAllTests {

		@Test
		@DisplayName("getAll_AdminRequest_ReturnsAllNotifications")
		void getAll_AdminRequest_ReturnsAllNotifications() {
			when(notificationRepository.findAll()).thenReturn(List.of(sampleNotification));
			when(notificationMapper.toResponseList(anyList())).thenReturn(List.of(sampleResponseDTO));

			List<ResponseDTO> result = notificationService.getAll();

			assertThat(result).hasSize(1);
			verify(notificationRepository).findAll();
		}
	}

	// ===================================================================
	// deleteNotification
	// ===================================================================

	@Nested
	@DisplayName("deleteNotification()")
	class DeleteNotificationTests {

		@Test
		@DisplayName("deleteNotification_ExistingId_DeletesSuccessfully")
		void deleteNotification_ExistingId_DeletesSuccessfully() {
			when(notificationRepository.findById(1)).thenReturn(Optional.of(sampleNotification));

			notificationService.deleteNotification(1);

			verify(notificationRepository).deleteByNotificationId(1);
		}

		@Test
		@DisplayName("deleteNotification_NotFound_ThrowsNotificationNotFoundException")
		void deleteNotification_NotFound_ThrowsNotificationNotFoundException() {
			when(notificationRepository.findById(999)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> notificationService.deleteNotification(999))
					.isInstanceOf(NotificationNotFoundException.class);

			verify(notificationRepository, never()).deleteByNotificationId(anyInt());
		}

		@Test
		@DisplayName("deleteNotification_DeleteThrows_WrapsInPersistenceException")
		void deleteNotification_DeleteThrows_WrapsInPersistenceException() {
			when(notificationRepository.findById(1)).thenReturn(Optional.of(sampleNotification));
			doThrow(new RuntimeException("DB error")).when(notificationRepository).deleteByNotificationId(1);

			assertThatThrownBy(() -> notificationService.deleteNotification(1))
					.isInstanceOf(NotificationPersistenceException.class);
		}
	}

	// ===================================================================
	// sendEmailAlert
	// ===================================================================

	@Nested
	@DisplayName("sendEmailAlert()")
	class SendEmailAlertTests {

		@Test
		@DisplayName("sendEmailAlert_ValidInput_CallsMailSender")
		void sendEmailAlert_ValidInput_CallsMailSender() {
			doNothing().when(mailSender).send(any(MimeMessage.class));

			notificationService.sendEmailAlert("user@test.com", "Hello", "Body text");

			verify(mailSender, times(1)).send(any(MimeMessage.class));
		}

		@Test
		@DisplayName("sendEmailAlert_MailServerDown_ThrowsEmailNotificationException")
		void sendEmailAlert_MailServerDown_ThrowsEmailNotificationException() {
			doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

			assertThatThrownBy(() -> notificationService.sendEmailAlert("fail@test.com", "Subject", "Body"))
					.isInstanceOf(EmailNotificationException.class).hasMessageContaining("fail@test.com");
		}

		@Test
		@DisplayName("sendEmailAlert_SetsCorrectFromAddress")
		void sendEmailAlert_SetsCorrectFromAddress() {
			doNothing().when(mailSender).send(any(MimeMessage.class));

			notificationService.sendEmailAlert("to@test.com", "Subject", "Body");

			verify(mailSender).send(argThat((MimeMessage msg) -> {
				try {
					return "no-reply@connectsphere.com".equals(msg.getFrom()[0].toString())
							&& "to@test.com".equals(msg.getAllRecipients()[0].toString())
							&& "Subject".equals(msg.getSubject());
				} catch (Exception ex) {
					return false;
				}
			}));
		}
	}
}
