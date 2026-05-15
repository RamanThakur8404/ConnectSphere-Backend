package com.connectsphere.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import com.connectsphere.auth.config.RabbitMQConfig;
import com.connectsphere.auth.constant.ErrorMessages;
import com.connectsphere.auth.constant.LogMessages;
import com.connectsphere.auth.constant.Role;
import com.connectsphere.auth.dto.LoginResponse;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateUserRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.event.UserDeactivatedEvent;
import com.connectsphere.auth.event.UserRegisteredEvent;
import com.connectsphere.auth.exception.AuthenticationFailedException;
import com.connectsphere.auth.exception.DuplicateResourceException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.mapper.UserMapper;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;

import lombok.RequiredArgsConstructor;

// Core implementation of {@link AuthService}.
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;
	private final PasswordEncoder passwordEncoder;
	private final RedisTokenService redisTokenService;
	private final RabbitTemplate rabbitTemplate;
	private final JavaMailSender mailSender;

	@Value("${app.frontend.base-url:http://localhost:5173}")
	private String frontendBaseUrl;

	@Value("${spring.mail.username:no-reply@connectsphere.com}")
	private String mailFromAddress = "no-reply@connectsphere.com";

	// -----------------------------------------------------------------------
	// Registration
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public UserResponse register(RegisterRequest request) {
		log.info(LogMessages.REGISTER_ATTEMPT, request.getEmail());

		if (userRepository.existsByEmail(request.getEmail())) {
			log.warn(LogMessages.REGISTER_EMAIL_TAKEN, request.getEmail());
			throw new DuplicateResourceException(ErrorMessages.EMAIL_ALREADY_REGISTERED);
		}
		if (userRepository.existsByUsername(request.getUsername())) {
			log.warn(LogMessages.REGISTER_USERNAME_TAKEN, request.getUsername());
			throw new DuplicateResourceException(ErrorMessages.USERNAME_ALREADY_TAKEN);
		}

		User user = UserMapper.toEntity(request);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setRole(Role.USER);

		User saved = userRepository.save(user);
		log.info(LogMessages.REGISTER_SUCCESS, saved.getUserId());

		// Publish event so notification-service can send a welcome email
		publishUserRegisteredEvent(saved);

		return UserMapper.toResponse(saved);
	}

	// -----------------------------------------------------------------------
	// Login
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public LoginResponse login(String email, String password) {
		log.info(LogMessages.LOGIN_ATTEMPT, email);

		User user = userRepository.findByEmail(email).orElseThrow(() -> {
			log.warn(LogMessages.LOGIN_USER_NOT_FOUND, email);
			return new AuthenticationFailedException(ErrorMessages.INVALID_CREDENTIALS);
		});

		if (!user.getActive()) {
			log.warn(LogMessages.ACCOUNT_DEACTIVATED, email);
			throw new AuthenticationFailedException(ErrorMessages.ACCOUNT_DEACTIVATED);
		}

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			log.warn(LogMessages.LOGIN_WRONG_PASSWORD, email);
			throw new AuthenticationFailedException(ErrorMessages.INVALID_CREDENTIALS);
		}

		return generateLoginResponseAndTokens(user);
	}

	// -----------------------------------------------------------------------
	// OTP Login
	// -----------------------------------------------------------------------

	@Override
	public void requestLoginOtp(String email) {
		log.info("OTP login requested for email: {}", email);
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) {
			log.warn("OTP login skipped because no account exists for email: {}", email);
			return;
		}

		if (!user.getActive()) {
			log.warn(LogMessages.ACCOUNT_DEACTIVATED, email);
			throw new AuthenticationFailedException(ErrorMessages.ACCOUNT_DEACTIVATED);
		}

		// Generate 6-digit OTP securely
		java.security.SecureRandom random = new java.security.SecureRandom();
		String otp = String.format("%06d", random.nextInt(999999));
		redisTokenService.storeOtp(email, otp);

		// Send Email
		try {
			sendBrandedEmail(email, "Your Login OTP for ConnectSphere",
					"Your one-time password for login is: " + otp
							+ "\nThis code will expire in 5 minutes.",
					buildOtpEmailHtml(otp));
			log.info("OTP Email sent to: {}", email);
		} catch (Exception e) {
			log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
		}
	}

	@Override
	@Transactional
	public LoginResponse verifyLoginOtp(String email, String otp) {
		log.info("Verifying OTP login for email: {}", email);

		String cachedOtp = redisTokenService.getOtp(email);
		if (cachedOtp == null || !cachedOtp.equals(otp)) {
			log.warn("Invalid or expired OTP for email: {}", email);
			throw new AuthenticationFailedException("Invalid or expired OTP.");
		}

		// Successful validation - cleanup cache
		redisTokenService.deleteOtp(email);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_EMAIL + email));

		if (!user.getActive()) {
			throw new AuthenticationFailedException(ErrorMessages.ACCOUNT_DEACTIVATED);
		}

		return generateLoginResponseAndTokens(user);
	}

	private LoginResponse generateLoginResponseAndTokens(User user) {
		user.setLastLoginAt(LocalDateTime.now());
		userRepository.save(user);

		String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId());
		String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getRole().name(), user.getUserId());

		// Persist refresh token in Redis (TTL = 7 days)
		redisTokenService.storeRefreshToken(user.getEmail(), refreshToken);

		log.info(LogMessages.LOGIN_SUCCESS, user.getEmail());
		return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken).role(user.getRole().name())
				.userId(user.getUserId()).build();
	}

	// -----------------------------------------------------------------------
	// Logout
	// -----------------------------------------------------------------------

	@Override
	public void logout(String accessToken, String refreshToken) {
		log.info("Logout attempt with tokens");

		// Blacklist the access token (TTL = remaining lifetime of the token)
		if (accessToken != null && !accessToken.isBlank()) {
			redisTokenService.blacklistAccessToken(accessToken);
		}

		String email = null;
		
		if (refreshToken != null && !refreshToken.isBlank()) {
			try {
				email = jwtUtil.extractEmail(refreshToken);
			} catch (Exception e) {
				log.warn("Failed to extract email from refresh token");
			}
		}
		
		if (email == null && accessToken != null && !accessToken.isBlank()) {
			try {
				email = jwtUtil.extractEmail(accessToken);
			} catch (io.jsonwebtoken.ExpiredJwtException e) {
				if (e.getClaims() != null) {
					email = e.getClaims().getSubject();
				}
			} catch (Exception e) {
				log.warn("Failed to extract email from access token");
			}
		}

		// Remove the stored refresh token so it can no longer be used
		if (email != null && !email.isBlank()) {
			redisTokenService.deleteRefreshToken(email);
			log.info(LogMessages.LOGOUT_SUCCESS, email);
		}
	}

	// -----------------------------------------------------------------------
	// Token Refresh (rolling refresh)
	// -----------------------------------------------------------------------

	@Override
	public LoginResponse refreshToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new AuthenticationFailedException(ErrorMessages.REFRESH_TOKEN_BLANK);
		}

		// Reject structurally invalid / expired tokens
		if (!jwtUtil.validateToken(refreshToken)) {
			throw new AuthenticationFailedException(ErrorMessages.REFRESH_TOKEN_INVALID);
		}

		// Reject blacklisted tokens
		if (redisTokenService.isBlacklisted(refreshToken)) {
			throw new AuthenticationFailedException(ErrorMessages.REFRESH_TOKEN_REVOKED);
		}

		String email = jwtUtil.extractEmail(refreshToken);

		// Verify the stored refresh token matches (prevents re-use of old tokens)
		String stored = redisTokenService.getRefreshToken(email);
		if (!refreshToken.equals(stored)) {
			throw new AuthenticationFailedException(ErrorMessages.REFRESH_TOKEN_ROTATED);
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_EMAIL + email));

		if (!user.getActive()) {
			throw new AuthenticationFailedException(ErrorMessages.ACCOUNT_DEACTIVATED);
		}

		// Issue new tokens (rolling refresh — old refresh token is replaced)
		String newAccessToken = jwtUtil.generateToken(email, user.getRole().name(), user.getUserId());
		String newRefreshToken = jwtUtil.generateRefreshToken(email, user.getRole().name(), user.getUserId());

		redisTokenService.storeRefreshToken(email, newRefreshToken);

		log.info(LogMessages.TOKEN_REFRESH_SUCCESS, email);
		return LoginResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken)
				.role(user.getRole().name()).userId(user.getUserId()).build();
	}

	// -----------------------------------------------------------------------
	// User retrieval
	// -----------------------------------------------------------------------

	@Override
	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_EMAIL + email));
	}

	@Override
	public User getUserById(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_ID + userId));
	}

	@Override
	public User getUserByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
	}

	@Override
	public UserResponse getUserProfile(String email) {
		return UserMapper.toResponse(getUserByEmail(email));
	}

	// -----------------------------------------------------------------------
	// Profile & password update
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public UserResponse updateUserProfile(String email, UpdateUserRequest request) {
		User user = getUserByEmail(email);

		if (request.getUsername() != null && !request.getUsername().isBlank()) {
			// Ensure the new username is not already taken by someone else
			if (userRepository.existsByUsername(request.getUsername())
					&& !user.getUsername().equals(request.getUsername())) {
				throw new DuplicateResourceException(ErrorMessages.USERNAME_ALREADY_TAKEN);
			}
			user.setUsername(request.getUsername());
		}
		if (request.getFullName() != null)
			user.setFullName(request.getFullName());
		if (request.getBio() != null)
			user.setBio(request.getBio());
		if (request.getProfilePicUrl() != null)
			user.setProfilePicUrl(request.getProfilePicUrl());

		log.info(LogMessages.PROFILE_UPDATED, email);
		return UserMapper.toResponse(userRepository.save(user));
	}

	@Override
	@Transactional
	public void changeUserPassword(String email, String currentPassword, String newPassword) {
		User user = getUserByEmail(email);

		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new AuthenticationFailedException(ErrorMessages.CURRENT_PASSWORD_WRONG);
		}

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		log.info(LogMessages.PASSWORD_CHANGED, email);
	}

	@Override
	public void forgetPassword(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_EMAIL + email));

		if (!user.getActive()) {
			throw new AuthenticationFailedException(ErrorMessages.ACCOUNT_DEACTIVATED);
		}

		String resetToken = java.util.UUID.randomUUID().toString();
		redisTokenService.storeResetToken(resetToken, email);

		sendResetPasswordEmail(email, resetToken);
		log.info(LogMessages.FORGET_PASSWORD_EMAIL_SENT, email);
	}

	@Override
	@Transactional
	public void resetPassword(String token, String newPassword) {
		String email = redisTokenService.getResetToken(token);
		if (email == null) {
			throw new AuthenticationFailedException(ErrorMessages.INVALID_RESET_TOKEN);
		}

		User user = getUserByEmail(email);
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);

		redisTokenService.deleteResetToken(token);
		log.info(LogMessages.PASSWORD_RESET_SUCCESS, email);
	}

	// -----------------------------------------------------------------------
	// Account deactivation
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public void deactivateUserAccount(String adminEmail, Long userId) {
		validateAdminAccountChange(adminEmail, userId);

		User target = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_ID + userId));

		target.setActive(false);
		userRepository.save(target);
		log.info(LogMessages.ACCOUNT_DEACTIVATED, userId, adminEmail);

		// Publish deactivation event so other services can hide the user's content
		publishUserDeactivatedEvent(target, adminEmail);
	}

	@Override
	@Transactional
	public void activateUserAccount(String adminEmail, Long userId) {
		validateAdminAccountChange(adminEmail, userId);

		User target = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_ID + userId));

		target.setActive(true);
		userRepository.save(target);
		log.info("Account activated - userId: {} by admin: {}", userId, adminEmail);
	}

	private void validateAdminAccountChange(String adminEmail, Long userId) {
		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.ADMIN_NOT_FOUND));

		if (admin.getRole() != Role.ADMIN) {
			throw new AuthenticationFailedException(ErrorMessages.ONLY_ADMIN_DEACTIVATE);
		}
		if (admin.getUserId().equals(userId)) {
			throw new IllegalArgumentException(ErrorMessages.ADMIN_SELF_DEACTIVATE);
		}
	}

	// -----------------------------------------------------------------------
	// Search
	// -----------------------------------------------------------------------

	@Override
	public List<UserResponse> searchUsers(String query) {
		return userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(query, query).stream()
				.map(UserMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
				.map(UserMapper::toResponse)
				.toList();
	}

	// -----------------------------------------------------------------------
	// Privileged user creation (admin-only)
	// -----------------------------------------------------------------------

	@Override
	@Transactional
	public User createPrivilegedUser(User user, Role role) {
		if (role == Role.USER) {
			throw new IllegalArgumentException(ErrorMessages.CANNOT_CREATE_USER_ROLE);
		}
		if (userRepository.existsByEmail(user.getEmail())) {
			throw new DuplicateResourceException(ErrorMessages.EMAIL_ALREADY_EXISTS);
		}
		if (userRepository.existsByUsername(user.getUsername())) {
			throw new DuplicateResourceException(ErrorMessages.USERNAME_ALREADY_EXISTS);
		}
		user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
		user.setRole(role);
		return userRepository.save(user);
	}

	// -----------------------------------------------------------------------
	// RabbitMQ event helpers
	// -----------------------------------------------------------------------

	private void publishUserRegisteredEvent(User user) {
		try {
			UserRegisteredEvent event = UserRegisteredEvent.builder().userId(user.getUserId()).email(user.getEmail())
					.username(user.getUsername()).fullName(user.getFullName()).build();
			rabbitTemplate.convertAndSend(RabbitMQConfig.AUTH_EXCHANGE, RabbitMQConfig.ROUTING_KEY_USER_REGISTERED,
					event);
			log.info(LogMessages.REGISTER_EVENT_PUBLISHED, user.getUserId());
		} catch (Exception e) {
			// Non-critical — log and continue; don't fail the registration
			log.error(LogMessages.REGISTER_EVENT_FAILED, e.getMessage());
		}
	}

	private void publishUserDeactivatedEvent(User user, String adminEmail) {
		try {
			UserDeactivatedEvent event = UserDeactivatedEvent.builder().userId(user.getUserId()).email(user.getEmail())
					.deactivatedByAdminEmail(adminEmail).build();
			rabbitTemplate.convertAndSend(RabbitMQConfig.AUTH_EXCHANGE, RabbitMQConfig.ROUTING_KEY_USER_DEACTIVATED,
					event);
			log.info(LogMessages.DEACTIVATE_EVENT_PUBLISHED, user.getUserId());
		} catch (Exception e) {
			log.error(LogMessages.DEACTIVATE_EVENT_FAILED, e.getMessage());
		}
	}

	private void sendResetPasswordEmail(String email, String resetToken) {
		try {
			String resetUrl = frontendBaseUrl + "/auth/reset-password?token=" + resetToken;
			sendBrandedEmail(email, "Reset Your Password",
					"Click the link to reset your password: " + resetUrl
							+ "\n\nThis link will expire in 15 minutes."
							+ "\nIf you didn't request this, please ignore this email.",
					buildResetPasswordEmailHtml(resetUrl));
			log.info("Reset password email sent to {}", email);
		} catch (Exception e) {
			log.error("Failed to send reset password email to {}: {}", email, e.getMessage());
		}
	}

	private void sendBrandedEmail(String toEmail, String subject, String plainText, String htmlBody)
			throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
		helper.setTo(toEmail);
		helper.setSubject(subject);
		helper.setFrom(mailFromAddress);
		helper.setText(plainText, htmlBody);
		mailSender.send(message);
	}

	private String buildOtpEmailHtml(String otp) {
		String safeOtp = escapeHtml(otp);
		return """
				<!DOCTYPE html>
				<html>
				  <body style="margin:0;background:#eef3f8;font-family:Arial,Helvetica,sans-serif;color:#172033;">
				    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#eef3f8;padding:34px 12px;">
				      <tr>
				        <td align="center">
				          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #dbe5f1;border-radius:22px;overflow:hidden;box-shadow:0 18px 50px rgba(15,23,42,.12);">
				            <tr>
				              <td style="background:#0f172a;padding:32px 34px;color:#ffffff;">
				                <div style="font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#bfdbfe;">ConnectSphere</div>
				                <h1 style="margin:16px 0 8px;font-size:28px;line-height:1.2;">Verify your login</h1>
				                <p style="margin:0;color:#cbd5e1;font-size:16px;line-height:1.6;">Use this one-time code to continue signing in.</p>
				              </td>
				            </tr>
				            <tr>
				              <td style="padding:34px;text-align:center;">
				                <div style="display:inline-block;padding:18px 28px;background:#f8fafc;border:1px solid #dbe5f1;border-radius:16px;color:#0f172a;font-size:34px;font-weight:800;letter-spacing:.18em;">%s</div>
				                <p style="margin:24px 0 0;color:#475569;font-size:15px;line-height:1.6;">This code expires in 5 minutes. Do not share it with anyone.</p>
				              </td>
				            </tr>
				            <tr>
				              <td style="padding:20px 34px;background:#f8fafc;color:#64748b;font-size:13px;line-height:1.5;">If you did not request this login code, you can safely ignore this email.</td>
				            </tr>
				          </table>
				        </td>
				      </tr>
				    </table>
				  </body>
				</html>
				""".formatted(safeOtp);
	}

	private String buildResetPasswordEmailHtml(String resetUrl) {
		String safeResetUrl = escapeHtml(resetUrl);
		return """
				<!DOCTYPE html>
				<html>
				  <body style="margin:0;background:#eef3f8;font-family:Arial,Helvetica,sans-serif;color:#172033;">
				    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#eef3f8;padding:34px 12px;">
				      <tr>
				        <td align="center">
				          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #dbe5f1;border-radius:22px;overflow:hidden;box-shadow:0 18px 50px rgba(15,23,42,.12);">
				            <tr>
				              <td style="background:#0f172a;padding:32px 34px;color:#ffffff;">
				                <div style="font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:#bfdbfe;">ConnectSphere</div>
				                <h1 style="margin:16px 0 8px;font-size:28px;line-height:1.2;">Reset your password</h1>
				                <p style="margin:0;color:#cbd5e1;font-size:16px;line-height:1.6;">We received a request to help you get back into your account.</p>
				              </td>
				            </tr>
				            <tr>
				              <td style="padding:34px;">
				                <p style="margin:0 0 22px;color:#334155;font-size:16px;line-height:1.65;">Use the button below to choose a new password. The link expires in 15 minutes.</p>
				                <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;font-weight:700;font-size:15px;padding:14px 22px;border-radius:12px;">Reset password</a>
				                <p style="margin:24px 0 0;color:#64748b;font-size:13px;line-height:1.6;">If the button does not work, paste this link into your browser:<br><span style="word-break:break-all;color:#334155;">%s</span></p>
				              </td>
				            </tr>
				            <tr>
				              <td style="padding:20px 34px;background:#f8fafc;color:#64748b;font-size:13px;line-height:1.5;">If you did not request this, you can safely ignore this email.</td>
				            </tr>
				          </table>
				        </td>
				      </tr>
				    </table>
				  </body>
				</html>
				""".formatted(safeResetUrl, safeResetUrl);
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
