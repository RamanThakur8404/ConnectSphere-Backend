package com.connectsphere.follow_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Data Transfer Object for creating a new follow relationship.
public class FollowRequestDTO {

    // The ID of the user who wants to follow someone.
    @NotNull(message = "Follower ID is required")
    @Positive(message = "Follower ID must be a positive integer")
    private Integer followerId;

    // The ID of the user to be followed.
    @NotNull(message = "Followee ID is required")
    @Positive(message = "Followee ID must be a positive integer")
    private Integer followeeId;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FollowRequestDTO() {
    }

    public FollowRequestDTO(Integer followerId, Integer followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Integer getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Integer followerId) {
        this.followerId = followerId;
    }

    public Integer getFolloweeId() {
        return followeeId;
    }

    public void setFolloweeId(Integer followeeId) {
        this.followeeId = followeeId;
    }
}
