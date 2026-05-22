package com.connectsphere.auth.constant;

// Centralised error / exception message strings for AuthServiceImpl.
public final class ErrorMessages {

    private ErrorMessages() { /* constants-only class */ }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------
    public static final String EMAIL_ALREADY_REGISTERED  = "Email is already registered";
    public static final String USERNAME_ALREADY_TAKEN    = "Username is already taken";

    // -----------------------------------------------------------------------
    // Authentication
    // -----------------------------------------------------------------------
    public static final String INVALID_CREDENTIALS       = "Invalid email or password";
    public static final String ACCOUNT_DEACTIVATED       = "This account has been deactivated";
    public static final String REFRESH_TOKEN_BLANK       = "Refresh token must not be blank";
    public static final String REFRESH_TOKEN_INVALID     = "Refresh token is invalid or expired";
    public static final String REFRESH_TOKEN_REVOKED     = "Refresh token has been revoked";
    public static final String REFRESH_TOKEN_ROTATED     = "Refresh token does not match or has been rotated";
    public static final String CURRENT_PASSWORD_WRONG    = "Current password is incorrect";
    public static final String INVALID_RESET_TOKEN       = "Invalid or expired reset token";

    // -----------------------------------------------------------------------
    // User lookup
    // -----------------------------------------------------------------------
    public static final String USER_NOT_FOUND_EMAIL      = "User not found: ";      // append email at call site
    public static final String USER_NOT_FOUND_ID         = "User not found with id: "; // append id at call site
    public static final String ADMIN_NOT_FOUND           = "Admin not found";

    // -----------------------------------------------------------------------
    // Authorisation / business rules
    // -----------------------------------------------------------------------
    public static final String ONLY_ADMIN_DEACTIVATE     = "Only ADMIN can deactivate accounts";
    public static final String ADMIN_SELF_DEACTIVATE     = "Admin cannot deactivate their own account";
    public static final String CANNOT_CREATE_USER_ROLE   = "Cannot create USER via admin endpoint";
    public static final String EMAIL_ALREADY_EXISTS      = "Email already exists";
    public static final String USERNAME_ALREADY_EXISTS   = "Username already exists";
}