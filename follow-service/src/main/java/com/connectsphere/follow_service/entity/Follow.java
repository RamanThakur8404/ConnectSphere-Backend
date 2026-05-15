package com.connectsphere.follow_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

// Entity representing a directed follow relationship between two users.
@Entity
@Table(
    name = "follows",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_follower_followee",
            columnNames = {"follower_id", "followee_id"}
        )
    },
    indexes = {
        @Index(name = "idx_follower_id", columnList = "follower_id"),
        @Index(name = "idx_followee_id", columnList = "followee_id")
    }
)

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private int followId;

    // The ID of the user who is following (the actor).
    @NotNull(message = "Follower ID must not be null")
    @Positive(message = "Follower ID must be a positive integer")
    @Column(name = "follower_id", nullable = false)
    private int followerId;

    // The ID of the user being followed (the target).
    @NotNull(message = "Followee ID must not be null")
    @Positive(message = "Followee ID must be a positive integer")
    @Column(name = "followee_id", nullable = false)
    private int followeeId;

    // Status of the follow relationship.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private FollowStatus status = FollowStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Follow(int followerId, int followeeId, FollowStatus status) {
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.status = status;
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
