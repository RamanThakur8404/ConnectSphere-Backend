package com.connectsphere.comment_service.constant;

// Centralised log message templates for CommentServiceImpl.
public final class LogMessages {

    private LogMessages() { /* constants-only class */ }

    // -----------------------------------------------------------------------
    // Add Comment
    // -----------------------------------------------------------------------
    public static final String ADD_COMMENT_ATTEMPT         = "Adding comment for postId={} by authorId={} parentCommentId={}";
    public static final String ADD_COMMENT_SUCCESS         = "Comment created successfully — commentId={}";
    public static final String ADD_COMMENT_FEIGN_FAILED    = "Failed to increment comment count on post-service for postId={}: {}";
    public static final String ADD_COMMENT_FEIGN_SUCCESS   = "Post comment count incremented for postId={}";

    // -----------------------------------------------------------------------
    // Get Comments
    // -----------------------------------------------------------------------
    public static final String GET_BY_POST_ATTEMPT         = "Fetching all comments for postId={}";
    public static final String GET_BY_ID_ATTEMPT           = "Fetching comment with commentId={}";
    public static final String GET_REPLIES_ATTEMPT         = "Fetching replies for parentCommentId={}";
    public static final String GET_BY_USER_ATTEMPT         = "Fetching all comments by authorId={}";
    public static final String GET_COUNT_ATTEMPT           = "Counting comments for postId={}";

    // -----------------------------------------------------------------------
    // Update Comment
    // -----------------------------------------------------------------------
    public static final String UPDATE_COMMENT_ATTEMPT      = "Updating comment commentId={} by userId={}";
    public static final String UPDATE_COMMENT_SUCCESS      = "Comment commentId={} updated successfully";

    // -----------------------------------------------------------------------
    // Delete Comment
    // -----------------------------------------------------------------------
    public static final String DELETE_COMMENT_ATTEMPT      = "Soft-deleting comment commentId={} by userId={}";
    public static final String DELETE_COMMENT_SUCCESS      = "Comment commentId={} and {} replies soft-deleted";
    public static final String DELETE_COMMENT_FEIGN_FAILED = "Failed to decrement comment count on post-service for postId={}: {}";
    public static final String DELETE_COMMENT_FEIGN_SUCCESS = "Post comment count decremented for postId={}";

    // -----------------------------------------------------------------------
    // Like / Unlike
    // -----------------------------------------------------------------------
    public static final String LIKE_COMMENT_ATTEMPT        = "Liking comment commentId={}";
    public static final String LIKE_COMMENT_SUCCESS        = "Like count incremented for commentId={}";
    public static final String UNLIKE_COMMENT_ATTEMPT      = "Un-liking comment commentId={}";
    public static final String UNLIKE_COMMENT_SUCCESS      = "Like count decremented for commentId={}";

    // -----------------------------------------------------------------------
    // Authorisation
    // -----------------------------------------------------------------------
    public static final String UNAUTHORIZED_ATTEMPT        = "Unauthorized {} attempt on commentId={} by userId={}";
}
