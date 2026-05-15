package com.connectsphere.auth.constant;

// Centralised log message templates for AuthServiceImpl.
public final class LogMessages {

    private LogMessages() { /* constants-only class */ }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------
    public static final String REGISTER_ATTEMPT          = "Registering new user — email: {}";
    public static final String REGISTER_EMAIL_TAKEN      = "Registration failed — email already in use: {}";
    public static final String REGISTER_USERNAME_TAKEN   = "Registration failed — username taken: {}";
    public static final String REGISTER_SUCCESS          = "User registered successfully — userId: {}";
    public static final String REGISTER_EVENT_PUBLISHED  = "Published UserRegisteredEvent for userId: {}";
    public static final String REGISTER_EVENT_FAILED     = "Failed to publish UserRegisteredEvent: {}";

    // -----------------------------------------------------------------------
    // Login
    // -----------------------------------------------------------------------
    public static final String LOGIN_ATTEMPT             = "Login attempt — email: {}";
    public static final String LOGIN_USER_NOT_FOUND      = "Login failed — user not found: {}";
    public static final String LOGIN_ACCOUNT_DEACTIVATED = "Login denied — account deactivated: {}";
    public static final String LOGIN_WRONG_PASSWORD      = "Login failed — wrong password for: {}";
    public static final String LOGIN_SUCCESS             = "Login successful — email: {}";

    // -----------------------------------------------------------------------
    // Logout
    // -----------------------------------------------------------------------
    public static final String LOGOUT_ATTEMPT            = "Logout — email: {}";
    public static final String LOGOUT_SUCCESS            = "Logout complete — email: {}";

    // -----------------------------------------------------------------------
    // Token refresh
    // -----------------------------------------------------------------------
    public static final String TOKEN_REFRESH_SUCCESS     = "Token refreshed — email: {}";

    // -----------------------------------------------------------------------
    // Profile update
    // -----------------------------------------------------------------------
    public static final String PROFILE_UPDATED           = "Profile updated — email: {}";

    // -----------------------------------------------------------------------
    // Password change
    // -----------------------------------------------------------------------
    public static final String PASSWORD_CHANGED          = "Password changed — email: {}";
    public static final String FORGET_PASSWORD_EMAIL_SENT = "Forget password email sent — email: {}";
    public static final String PASSWORD_RESET_SUCCESS    = "Password reset successful — email: {}";

    // -----------------------------------------------------------------------
    // Account deactivation
    // -----------------------------------------------------------------------
    public static final String ACCOUNT_DEACTIVATED       = "Account deactivated — userId: {} by admin: {}";
    public static final String DEACTIVATE_EVENT_PUBLISHED = "Published UserDeactivatedEvent for userId: {}";
    public static final String DEACTIVATE_EVENT_FAILED   = "Failed to publish UserDeactivatedEvent: {}";
}