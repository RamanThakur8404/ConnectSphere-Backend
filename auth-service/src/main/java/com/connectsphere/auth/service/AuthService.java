package com.connectsphere.auth.service;

import java.util.List;

import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.dto.LoginResponse;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateUserRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.User;

// Service interface for handling authentication and user-related operations.
public interface AuthService {

	// Registers a new user in the system.

	UserResponse register(RegisterRequest request);

	// Authenticates a user using email and password.
	LoginResponse login(String email, String password);

	// Requests a 6-digit OTP for passwordless login.
	void requestLoginOtp(String email);

	// Verifies the OTP and logs the user in if valid.
	LoginResponse verifyLoginOtp(String email, String otp);

	// Logs out a user by invalidating the given token.
	void logout(String accessToken, String refreshToken);

	// Generates a new access token using a refresh token.
	LoginResponse refreshToken(String refreshToken);

	// =========================================================================
	// 2. USER MANAGEMENT
	// =========================================================================

//	/**
//	 * Validates whether the given token is valid or not.
//	 *
//	 * @param token the authentication token
//	 * @return true if token is valid, otherwise false
//	 */
//	boolean validateToken(String token);

	// Retrieves a user by email.
	User getUserByEmail(String email);

	// Retrieves a user by their unique ID.
	User getUserById(Long userId);

	// Retrieves a user by their public username.
	User getUserByUsername(String username);

	// Updates user profile details.
	UserResponse updateUserProfile(String email, UpdateUserRequest user);

	// Changes the password of a user.
	void changeUserPassword(String email, String currentPassword, String newPassword);

	// Initiates the forget password process by sending a reset email.
	void forgetPassword(String email);

	// Resets the user's password using the reset token.
	void resetPassword(String token, String newPassword);

	// Deactivates a user's account.
	void deactivateUserAccount(String adminEmail, Long userId);

	// Activates a user's account.
	void activateUserAccount(String adminEmail, Long userId);

	UserResponse getUserProfile(String email);

	// Lists all users for admin user management.
	List<UserResponse> getAllUsers();

	// Searches users based on a keyword (e.g., username).
	List<UserResponse> searchUsers(String query);

	// =========================================================================
	// 3. ROLE MANAGEMENT (Admin-only operations)
	// =========================================================================

	// Creates a new user with a specific role (ADMIN or MODERATOR). This method is
	User createPrivilegedUser(User user, Role role);
}
