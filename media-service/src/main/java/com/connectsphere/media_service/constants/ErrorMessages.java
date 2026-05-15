package com.connectsphere.media_service.constants;
public final class ErrorMessages {

    private ErrorMessages() { /* constants-only class */ }

    // Media
    public static final String MEDIA_NOT_FOUND         = "Media not found with id: ";
    public static final String MEDIA_ALREADY_DELETED   = "Media has already been deleted";
    public static final String MEDIA_INVALID_TYPE      = "Unsupported media type";
    public static final String MEDIA_PERSIST_FAILED    = "Failed to persist media";
    public static final String MEDIA_DELETE_FAILED     = "Failed to delete media";

    // Story
    public static final String STORY_NOT_FOUND         = "Story not found or has expired with id: ";
    public static final String STORY_PERSIST_FAILED    = "Failed to persist story";
    public static final String STORY_DELETE_FAILED     = "Failed to deactivate story";
    public static final String STORY_VIEW_FAILED       = "Failed to record story view";

    // Security
    public static final String MISSING_USER_ID_HEADER  = "X-User-Id header is missing or invalid";
    public static final String ACCESS_DENIED           = "You do not have permission to perform this action";
    public static final String UNAUTHORIZED_ACCESS     = "User is not authorised to modify this resource";

    // Generic
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String MALFORMED_REQUEST = "Malformed request body";
    public static final String MAX_UPLOAD_SIZE_EXCEEDED = "Uploaded file exceeds the allowed size limit";
    public static final String INTERNAL_ERROR    = "An unexpected error occurred. Please try again later.";
}
