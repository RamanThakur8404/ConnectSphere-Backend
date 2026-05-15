package com.connectsphere.auth.entity;

import java.time.LocalDateTime;

import com.connectsphere.auth.constant.AuthProvider;
import com.connectsphere.auth.constant.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Entity representing a User in the system.
@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(name = "uk_name", columnNames = "username"),
		@UniqueConstraint(name = "uk_email", columnNames = "email"), }, indexes = {
				@Index(name = "idx_username", columnList = "username"),
				@Index(name = "idx_email", columnList = "email") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	// Primary key for the user. Auto-generated using database identity strategy.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	// Unique username used for identification. Must be between 3 and 50 characters.
	@Column(nullable = false, length = 50, unique = true)
	private String username;

	// Unique email used for login and communication.
	@Column(nullable = false, length = 100, unique = true)
	private String email;

	// Stores hashed password.
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Column(nullable = false, length = 255)
	private String passwordHash;

	// Full name of the user (optional).
	@Column(length = 100)
	private String fullName;

	// Short bio or description of the user profile.
	@Column(length = 500)
	private String bio;

	// URL of user's profile picture
	@Column(length = 500)
	private String profilePicUrl;

	// Role of the user (e.g., USER, ADMIN). Default is USER.
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Role role = Role.USER;

	// Authentication provider (GOOGLE, LOCAL).
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private AuthProvider provider = AuthProvider.LOCAL;

	// Indicates whether the user account is active. Used for soft-deactivation

	@Column(name = "is_active", nullable = false)
	@Builder.Default
	private Boolean active = true;

	// Timestamp when the user account was created. Automatically set before
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Timestamp of the user's most recent successful login.
	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	// Two-factor authentication enabled flag.
	@Column(nullable = false)
	@Builder.Default
	private boolean twoFactorEnabled = false;

	// Indicates whether the user has verified their email address via OTP.
	@Column(nullable = false)
	@Builder.Default
	private boolean emailVerified = false;

	// Indicates whether the user holds an active premium subscription
	@Column(nullable = false)
	@Builder.Default
	private boolean isPremium = false;
	
	// Lifecycle callback method triggered before saving entity. Ensures default

	@PrePersist
	public void beforeSave() {

		// Set creation timestamp if not already set
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}

		if (this.active == null) {
			this.active = true;
		}
	}
}
