package com.connectsphere.follow_service.dto;

// Data Transfer Object that carries aggregated follower / following counts
public class FollowCountDTO {

    private int userId;
    private int followerCount;
    private int followingCount;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FollowCountDTO() {
    }

    public FollowCountDTO(int userId, int followerCount, int followingCount) {
        this.userId        = userId;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }
}
