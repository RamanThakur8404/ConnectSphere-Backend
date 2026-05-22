package com.connectsphere.notification_service.entity;


import java.time.LocalDateTime;

import com.connectsphere.notification_service.constants.NotificationTarget;
import com.connectsphere.notification_service.constants.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// JPA entity representing a single in-app notification record.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_recipient",        columnList = "recipient_id"),
        @Index(name = "idx_notif_recipient_unread",  columnList = "recipient_id, is_read"),
        @Index(name = "idx_notif_actor_target",      columnList = "actor_id, target_id")
    }
)
public class Notification {

    //  Primary Key ─────────────────────────────────────────────────────────

    // Auto-generated surrogate primary key.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private int notificationId;

    //  Participants ─────────────────────────────────────────────────────────

    // ID of the user who should receive this notification.
    @Column(name = "recipient_id", nullable = false)
    private int recipientId;

    // ID of the user whose action triggered this notification.
    @Column(name = "actor_id", nullable = false)
    private int actorId;

    //  Event Classification ─────────────────────────────────────────────────

    // Type of notification event.
    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // Human-readable notification message shown in the alert feed.
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    //  Deep-link Context ────────────────────────────────────────────────────

    // ID of the entity (post or comment) associated with this notification.
    @Column(name = "target_id")
    private Integer targetId;

    // Type of the target entity: POST, COMMENT, or USER.
    @Column(name = "target_type", length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationTarget targetType;

    // Full deep-link URL to navigate the recipient directly to the relevant
    @Column(name = "deep_link_url", length = 300)
    private String deepLinkUrl;

    //  State ────────────────────────────────────────────────────────────────

    // Whether the recipient has read this notification.
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    //  Audit ────────────────────────────────────────────────────────────────

    // Timestamp when the notification was created.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //  Lifecycle Callbacks ──────────────────────────────────────────────────

    // Sets {@link #createdAt} to the current date/time before the entity is
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
