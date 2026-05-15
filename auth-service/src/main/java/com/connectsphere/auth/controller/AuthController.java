package com.connectsphere.auth.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.dto.ChangePasswordRequest;
import com.connectsphere.auth.dto.ForgetPasswordRequest;
import com.connectsphere.auth.dto.LoginRequest;
import com.connectsphere.auth.dto.LoginResponse;
import com.connectsphere.auth.dto.RefreshTokenRequest;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.ResetPasswordRequest;
import com.connectsphere.auth.dto.UpdateUserRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.mapper.UserMapper;
import com.connectsphere.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// REST Controller for handling authentication and user-related operations.
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Value("${app.auth.cookie.secure:false}")
	private boolean cookieSecure;

	@Value("${app.auth.cookie.same-site:Lax}")
	private String cookieSameSite;

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);
	private static final int ACCESS_COOKIE_MAX_AGE  = 24 * 60 * 60;       // 24 h
    private static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;   // 7 days

	// ============= No Authentication required ============================
    @Operation(summary = "Register a new user account")
	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
    
    @Operation(summary = "Login with email and password; returns access + refresh tokens")
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    	LoginResponse loginResponse = authService.login(request.getEmail(), request.getPassword());

		// Set HttpOnly cookies for browser clients
        ResponseCookie accessCookie = buildCookie("jwt",          loginResponse.getAccessToken(),  ACCESS_COOKIE_MAX_AGE);
        ResponseCookie refreshCookie= buildCookie("refreshToken", loginResponse.getRefreshToken(), REFRESH_COOKIE_MAX_AGE);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginResponse);
	}

    @Operation(summary = "Request a 6-digit OTP for passwordless login")
    @PostMapping("/login/otp/send")
    public ResponseEntity<String> sendLoginOtp(@Valid @RequestBody com.connectsphere.auth.dto.LoginOtpRequest request) {
        authService.requestLoginOtp(request.getEmail());
        return ResponseEntity.ok("If the account exists, an OTP has been sent to your email.");
    }

    @Operation(summary = "Verify OTP and login; returns access + refresh tokens")
    @PostMapping("/login/otp/verify")
    public ResponseEntity<LoginResponse> verifyLoginOtp(@Valid @RequestBody com.connectsphere.auth.dto.LoginOtpVerifyRequest request) {
        LoginResponse loginResponse = authService.verifyLoginOtp(request.getEmail(), request.getOtp());

        // Set HttpOnly cookies for browser clients
        ResponseCookie accessCookie = buildCookie("jwt",          loginResponse.getAccessToken(),  ACCESS_COOKIE_MAX_AGE);
        ResponseCookie refreshCookie= buildCookie("refreshToken", loginResponse.getRefreshToken(), REFRESH_COOKIE_MAX_AGE);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginResponse);
    }
    
    @Operation(summary = "Get public profile for any user (no auth required)")
	@GetMapping("/users/public/{userId}")
	public ResponseEntity<Map<String, Object>> getPublicProfile(@PathVariable Long userId) {
		User user = authService.getUserById(userId);
		return ResponseEntity.ok(toPublicProfile(user));
	}

    @Operation(summary = "Get public profile by username (no auth required)")
	@GetMapping("/users/public/username/{username}")
	public ResponseEntity<Map<String, Object>> getPublicProfileByUsername(@PathVariable String username) {
		User user = authService.getUserByUsername(username);
		return ResponseEntity.ok(toPublicProfile(user));
	}

    @Operation(summary = "Search users by username or full name")
	@GetMapping("/search")
	public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String username) {
		return ResponseEntity.ok(authService.searchUsers(username));
	}

	// =========================================================================
	// AUTHENTICATED ENDPOINTS — Valid JWT required
	// =========================================================================

	// Logs out user by invalidating token and clearing the cookie.
    @Operation(summary = "Logout — blacklists current token and clears cookies")
	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpServletRequest request) {
		// Try to extract token from cookie first, then from header
		String token = extractAccessToken(request);
		String refreshToken = extractCookieValue(request, "refreshToken");
        authService.logout(token, refreshToken);

     // Expire both cookies
        ResponseCookie clearAccess  = buildCookie("jwt",          "", 0);
        ResponseCookie clearRefresh = buildCookie("refreshToken", "", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .body("Logged out successfully");
	}

	// Generates a new token using refresh token.
    @Operation(summary = "Refresh access token using a valid refresh token (rolling refresh)")
	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest body,HttpServletRequest request) {
    	 // Accept refresh token from request body OR from the HttpOnly cookie
        String refreshToken = body.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = extractCookieValue(request, "refreshToken");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        LoginResponse loginResponse = authService.refreshToken(refreshToken);

        ResponseCookie accessCookie  = buildCookie("jwt",          loginResponse.getAccessToken(),  ACCESS_COOKIE_MAX_AGE);
        ResponseCookie refreshCookie = buildCookie("refreshToken", loginResponse.getRefreshToken(), REFRESH_COOKIE_MAX_AGE);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginResponse);
	}

	// Retrieves user profile by user ID.
    @Operation(summary = "Get own profile", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/users/profile")
	public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
		String email = authentication.getName();
		UserResponse user = authService.getUserProfile(email);
		return ResponseEntity.ok(user);
	}

	// Updates user profile.
    @Operation(summary = "Update own profile", security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/users/update")
	public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
			@Valid @RequestBody UpdateUserRequest request) {
		String email = authentication.getName();
		return ResponseEntity.ok(authService.updateUserProfile(email, request));
	}

	// Changes user password.
    @Operation(summary = "Change own password", security = @SecurityRequirement(name = "bearerAuth"))
	@PatchMapping("/users/password")
	public ResponseEntity<String> changePassword(Authentication authentication,
			@RequestBody ChangePasswordRequest request) {
		String email = authentication.getName();
		String currentPassword = request.getCurrentPassword();
		String newPassword = request.getNewPassword();
		authService.changeUserPassword(email, currentPassword,newPassword);
		return ResponseEntity.ok("Password changed successfully");
	}

	// Initiates forget password process.
	@PostMapping("/forget-password")
	public ResponseEntity<String> forgetPassword(@Valid @RequestBody ForgetPasswordRequest request) {
		authService.forgetPassword(request.getEmail());
		return ResponseEntity.ok("If the email exists, a reset link has been sent.");
	}

	// Resets user password using token.
	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request.getToken(), request.getNewPassword());
		return ResponseEntity.ok("Password reset successfully.");
	}

	// =========================================================================
	// ADMIN-ONLY ENDPOINTS — JWT required AND role must be ADMIN
	// =========================================================================
    @Operation(summary = "List all users (ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/admin/users")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		return ResponseEntity.ok(authService.getAllUsers());
	}

    @Operation(summary = "Create a privileged user (ADMIN or MODERATOR role)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/admin/users/create")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> createPrivilegedUser(@Valid @RequestBody RegisterRequest request, @RequestParam Role role) {
		// Block attempts to use this endpoint to create regular USERs	
		User user = UserMapper.toEntity(request);
		User created = authService.createPrivilegedUser(user, role);
		return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(created));
	}

	// Deactivates a user account.
    @Operation(summary = "Deactivate a user account (ADMIN only)",
             security = @SecurityRequirement(name = "bearerAuth"))
	@PatchMapping("/admin/users/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> deactivateAccount(Authentication authentication, @RequestParam Long userId) {
		String adminEmail = authentication.getName();
		authService.deactivateUserAccount(adminEmail, userId);
		return ResponseEntity.ok("Account deactivated successfully");
	}

	// Activates a user account.
    @Operation(summary = "Activate a user account (ADMIN only)",
             security = @SecurityRequirement(name = "bearerAuth"))
	@PatchMapping("/admin/users/activate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> activateAccount(Authentication authentication, @RequestParam Long userId) {
		String adminEmail = authentication.getName();
		authService.activateUserAccount(adminEmail, userId);
		return ResponseEntity.ok("Account activated successfully");
	}

	// =========================================================================
	    // Helpers
	    // =========================================================================

	    private ResponseCookie buildCookie(String name, String value, int maxAge) {
	        return ResponseCookie.from(name, value)
	                .httpOnly(true)
	                .secure(cookieSecure)
	                .path("/")
	                .maxAge(maxAge)
	                .sameSite(cookieSameSite)
	                .build();
	    }

	    private String extractAccessToken(HttpServletRequest request) {
	        String fromCookie = extractCookieValue(request, "jwt");
	        if (fromCookie != null) return fromCookie;
	        String authHeader = request.getHeader("Authorization");
	        if (authHeader != null && authHeader.startsWith("Bearer ")) {
	            return authHeader.substring(7);
	        }
	        return null;
	    }

	    private String extractCookieValue(HttpServletRequest request, String cookieName) {
	        Cookie[] cookies = request.getCookies();
	        if (cookies != null) {
	            for (Cookie c : cookies) {
	                if (cookieName.equals(c.getName())) return c.getValue();
	            }
	        }
	        return null;
	    }

	    private Map<String, Object> toPublicProfile(User user) {
			// Return only safe, public fields; never return email or passwordHash.
			return Map.of("userId", user.getUserId(),
					"username", user.getUsername(),
					"fullName", user.getFullName() != null ? user.getFullName() : "",
					"bio", user.getBio() != null ? user.getBio() : "",
					"profilePicUrl", user.getProfilePicUrl() != null ? user.getProfilePicUrl() : "",
					"createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
	    }
}
