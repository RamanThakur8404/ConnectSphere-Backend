package com.connectsphere.auth.service;

import java.time.Duration;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.connectsphere.auth.security.JwtUtil;

import lombok.RequiredArgsConstructor;

// Service responsible for all Redis-backed token operations:
@Service
@RequiredArgsConstructor
public class RedisTokenService {

	private static final Logger log = LoggerFactory.getLogger(RedisTokenService.class);

	private static final String BLACKLIST_PREFIX = "blacklist:";
	private static final String REFRESH_PREFIX = "refresh:";
	private static final String RESET_PREFIX = "reset:";
	private static final String OTP_PREFIX = "otp:";

	private final StringRedisTemplate redisTemplate;
	private final JwtUtil jwtUtil;

	// -----------------------------------------------------------------------
	// Access-token blacklist
	// -----------------------------------------------------------------------

	// Blacklists an access token. TTL = remaining milliseconds until the token
	public void blacklistAccessToken(String token) {
		try {
			Date expiry = jwtUtil.extractExpiration(token);
			long ttlMs = expiry.getTime() - System.currentTimeMillis();
			if (ttlMs > 0) {
				redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", Duration.ofMillis(ttlMs));
				log.info("Access token blacklisted with TTL {}ms", ttlMs);
			}
		} catch (Exception e) {
			log.warn("Could not blacklist access token: {}", e.getMessage());
		}
	}

	// Returns true if the token has been blacklisted.
	public boolean isBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
	}

	// -----------------------------------------------------------------------
	// Refresh-token lifecycle
	// -----------------------------------------------------------------------

	// Persists a refresh token in Redis, keyed by email.
	public void storeRefreshToken(String email, String refreshToken) {
		long ttlMs = jwtUtil.getRefreshTokenExpiryMs();
		redisTemplate.opsForValue().set(REFRESH_PREFIX + email, refreshToken, Duration.ofMillis(ttlMs));
		log.debug("Stored refresh token for {}", email);
	}

	// Retrieves the stored refresh token for an email, or null if absent.
	public String getRefreshToken(String email) {
		return redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
	}

	// Deletes the refresh token for an email (called on logout).
	public void deleteRefreshToken(String email) {
		redisTemplate.delete(REFRESH_PREFIX + email);
		log.debug("Deleted refresh token for {}", email);
	}

	// -----------------------------------------------------------------------
	// Reset-token lifecycle
	// -----------------------------------------------------------------------

	// Stores a reset token in Redis, keyed by token, with TTL of 15 minutes.
	public void storeResetToken(String resetToken, String email) {
		redisTemplate.opsForValue().set(RESET_PREFIX + resetToken, email, Duration.ofMinutes(15));
		log.debug("Stored reset token for {}", email);
	}

	// Retrieves the email associated with a reset token, or null if absent.
	public String getResetToken(String resetToken) {
		return redisTemplate.opsForValue().get(RESET_PREFIX + resetToken);
	}

	// Deletes the reset token (called after password reset).
	public void deleteResetToken(String resetToken) {
		redisTemplate.delete(RESET_PREFIX + resetToken);
		log.debug("Deleted reset token");
	}

	// -----------------------------------------------------------------------
	// OTP lifecycle
	// -----------------------------------------------------------------------

	// Stores a login OTP in Redis, keyed by email, with TTL of 5 minutes.
	public void storeOtp(String email, String otp) {
		redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, Duration.ofMinutes(5));
		log.debug("Stored login OTP for {}", email);
	}

	// Retrieves the stored OTP for an email, or null if absent.
	public String getOtp(String email) {
		return redisTemplate.opsForValue().get(OTP_PREFIX + email);
	}

	// Deletes the OTP for an email.
	public void deleteOtp(String email) {
		redisTemplate.delete(OTP_PREFIX + email);
		log.debug("Deleted login OTP for {}", email);
	}
}