package com.connectsphere.follow_service.dto;

import com.connectsphere.follow_service.entity.FollowStatus;

import java.time.LocalDateTime;

// Data Transfer Object returned after a successful follow operation or
public class FollowResponseDTO {

    private int followId;
    private int followerId;
    private int followeeId;
    private FollowStatus status;
    private LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FollowResponseDTO() {
    }

    public FollowResponseDTO(int followId, int followerId, int followeeId,
                             FollowStatus status, LocalDateTime createdAt) {
        this.followId   = followId;
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.status     = status;
        this.createdAt  = createdAt;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getFollowId() {
        return followId;
    }

    public void setFollowId(int followId) {
        this.followId = followId;
    }

    public int getFollowerId() {
        return followerId;
    }

    public void setFollowerId(int followerId) {
        this.followerId = followerId;
    }

    public int getFolloweeId() {
        return followeeId;
    }

    public void setFolloweeId(int followeeId) {
        this.followeeId = followeeId;
    }

    public FollowStatus getStatus() {
        return status;
    }

    public void setStatus(FollowStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
