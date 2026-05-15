package com.connectsphere.notification_service.repository;

import com.connectsphere.notification_service.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

	//  By Recipient ─────────────────────────────────────────────────────────

	// Returns all notifications for a recipient, newest first, with pagination.
	Page<Notification> findByRecipientIdOrderByCreatedAtDesc(int recipientId, Pageable pageable);

	// Returns all notifications for a recipient ordered by creation time
	List<Notification> findByRecipientIdOrderByCreatedAtDesc(int recipientId);

	//  By Recipient and Read-State ─────────────────────────────────────────

	// Returns notifications filtered by recipient and read/unread state.
	List<Notification> findByRecipientIdAndIsRead(int recipientId, boolean isRead);

	// Counts the number of unread (or read) notifications for a recipient.
	int countByRecipientIdAndIsRead(int recipientId, boolean isRead);

	//  By Notification Type ────────────────────────────────────────────────

	// Returns all notifications of a specific type for a recipient.
	List<Notification> findByRecipientIdAndType(int recipientId, String type);

	// Returns all notifications of a specific type across all recipients. Used for
	List<Notification> findByType(String type);

	//  By Actor and Target ─────────────────────────────────────────────────

	// Checks whether a notification already exists for the given actor-target
	Optional<Notification> findByActorIdAndTargetIdAndType(int actorId, int targetId, String type);

	// Retrieves all notifications associated with a particular target entity.
	List<Notification> findByActorIdAndTargetId(int actorId, int targetId);

	//  Bulk Operations ─────────────────────────────────────────────────────

	// Marks all unread notifications for a recipient as read in a single bulk
	@Modifying
	@Transactional
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
	int markAllAsReadByRecipient(@Param("recipientId") int recipientId);

	// Deletes all notifications belonging to a specific recipient. Intended for
	@Modifying
	@Transactional
	@Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId")
	void deleteAllByRecipientId(@Param("recipientId") int recipientId);

	// Deletes a single notification by its primary key.
	void deleteByNotificationId(int notificationId);

	//  Existence Check ─────────────────────────────────────────────────────

	// Checks whether a notification record exists by primary key.
	boolean existsByNotificationId(int notificationId);
}
