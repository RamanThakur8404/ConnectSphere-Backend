package com.connectsphere.comment_service.constant;

// Centralised error / exception message strings for CommentServiceImpl.
public final class ErrorMessages {

    private ErrorMessages() { /* constants-only class */ }

    // -----------------------------------------------------------------------
    // Comment lookup
    // -----------------------------------------------------------------------
    public static final String COMMENT_NOT_FOUND           = "Comment not found with id: ";
    public static final String PARENT_COMMENT_NOT_FOUND    = "Parent comment not found with id: ";

    // -----------------------------------------------------------------------
    // Business rules
    // -----------------------------------------------------------------------
    public static final String REPLY_TO_REPLY_NOT_ALLOWED  =
            "Replies to replies are not allowed. Only two-level threading is supported.";
    public static final String UNAUTHORIZED_UPDATE         =
            "You are not authorized to update this comment.";
    public static final String UNAUTHORIZED_DELETE         =
            "You are not authorized to delete this comment.";

    // -----------------------------------------------------------------------
    // Header validation
    // -----------------------------------------------------------------------
    public static final String MISSING_USER_ID_HEADER      = "X-User-Id header is missing or invalid.";
}
