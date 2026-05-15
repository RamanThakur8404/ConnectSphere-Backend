package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegisterRequest {

	// Username: 3–30 chars, only letters, digits and underscores.
	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits and underscores")
	private String username;

	// Valid RFC-5322 email address.
	@NotBlank(message = "Email is required")
	@Email(message = "Email must be a valid address")
	@Pattern(
		    regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
		    message = "Email must follow standard format (example: user@gmail.com)"
		)
	private String email;

	// Password: at least 8 characters, must include one letter and one digit.
	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$", message = "Password must contain at least one letter and one digit")
	private String password;

	// Display name — 2 to 100 characters, optional at registration. 
	@Size(max = 100, message = "Full name must not exceed 100 characters")
	private String fullName;

	// Optional short biography — max 300 chars. 
	@Size(max = 300, message = "Bio must not exceed 300 characters")
	private String bio;

	// CDN URL for a profile picture — validated as URL format. 
	@Pattern(regexp = "^(https?://.*)?$", message = "Profile picture URL must be a valid http/https URL or empty")
	private String profilePicUrl;
}