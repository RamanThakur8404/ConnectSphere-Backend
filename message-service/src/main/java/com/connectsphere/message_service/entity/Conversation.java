package com.connectsphere.message_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conversations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long conversationId;

    // Always store the lower userId as user1 to enforce the unique constraint
    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.lastMessageAt = LocalDateTime.now();
    }
}
