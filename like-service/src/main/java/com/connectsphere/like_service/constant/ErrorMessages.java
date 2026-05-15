package com.connectsphere.like_service.constant;

// Centralised error / exception message strings for LikeServiceImpl.
public final class ErrorMessages {

    private ErrorMessages() { /* constants-only class */ }

    // -----------------------------------------------------------------------
    // Reaction not found
    // -----------------------------------------------------------------------
    public static final String REACTION_NOT_FOUND     = "No reaction found for user ";   // append context at call site
    public static final String REACTION_NOT_FOUND_ON  = " on ";

    // -----------------------------------------------------------------------
    // Duplicate reaction
    // -----------------------------------------------------------------------
    public static final String DUPLICATE_REACTION     = "User ";
    public static final String ALREADY_REACTED        = " has already reacted to ";

    // -----------------------------------------------------------------------
    // Authorization
    // -----------------------------------------------------------------------
    public static final String UNAUTHORIZED_ACTION    = "You are not authorized to perform this action";
    public static final String USER_ID_HEADER_MISSING = "X-User-Id header is missing or invalid";
}
