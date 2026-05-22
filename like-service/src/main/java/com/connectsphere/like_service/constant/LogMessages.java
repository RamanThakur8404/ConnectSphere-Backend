package com.connectsphere.like_service.constant;

// Centralised log message templates for LikeServiceImpl.
public final class LogMessages {

    private LogMessages() { /* constants-only class */ }

    // -----------------------------------------------------------------------
    // Like / React
    // -----------------------------------------------------------------------
    public static final String LIKE_ATTEMPT             = "User {} reacting to {} {} with {}";
    public static final String LIKE_SUCCESS             = "Reaction saved — likeId: {}";
    public static final String LIKE_DUPLICATE           = "Duplicate reaction by user {} on {} {}";
    public static final String LIKE_COUNT_INCREMENT_OK  = "Like count incremented on {} {}";
    public static final String LIKE_COUNT_INCREMENT_ERR = "Failed to increment like count on {}: {}";

    // -----------------------------------------------------------------------
    // Unlike
    // -----------------------------------------------------------------------
    public static final String UNLIKE_ATTEMPT           = "User {} removing reaction from {} {}";
    public static final String UNLIKE_SUCCESS           = "Reaction removed for user {} on {} {}";
    public static final String UNLIKE_NOT_FOUND         = "No reaction found for user {} on {} {}";
    public static final String LIKE_COUNT_DECREMENT_OK  = "Like count decremented on {} {}";
    public static final String LIKE_COUNT_DECREMENT_ERR = "Failed to decrement like count on {}: {}";

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------
    public static final String HAS_LIKED_CHECK          = "Checking if user {} has reacted to {} {}";
    public static final String GET_LIKES_BY_TARGET      = "Fetching all reactions for {} {}";
    public static final String GET_LIKES_BY_USER        = "Fetching all reactions by user {}";
    public static final String GET_LIKE_COUNT           = "Counting all reactions for {} {}";
    public static final String GET_LIKE_COUNT_BY_TYPE   = "Counting {} reactions for {} {}";
    public static final String GET_REACTION_SUMMARY     = "Building reaction summary for {} {}";

    // -----------------------------------------------------------------------
    // Change reaction
    // -----------------------------------------------------------------------
    public static final String CHANGE_REACTION_ATTEMPT  = "User {} changing reaction on {} {} to {}";
    public static final String CHANGE_REACTION_SUCCESS  = "Reaction {} updated to {}";
    public static final String CHANGE_REACTION_NOT_FOUND = "No existing reaction for user {} on {} {}";
}
