package com.connectsphere.follow_service.entity;

// Represents the possible states of a follow relationship.
public enum FollowStatus {

    // Follow request accepted; relationship is live. 
    ACTIVE,

    // Follow request submitted but not yet approved by the target user. 
    PENDING
}
