package com.connectsphere.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.connectsphere.auth.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisTokenService redisTokenService;

    // =========================================================================
    // Blacklist
    // =========================================================================

    @Test
    void blacklistAccessToken_WithValidToken() {
        Date futureDate = new Date(System.currentTimeMillis() + 60000); // 60s from now
        when(jwtUtil.extractExpiration("token123")).thenReturn(futureDate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        redisTokenService.blacklistAccessToken("token123");

        verify(valueOps).set(eq("blacklist:token123"), eq("1"), any(Duration.class));
    }

    @Test
    void blacklistAccessToken_WithExpiredToken_ShouldNotStore() {
        Date pastDate = new Date(System.currentTimeMillis() - 60000); // 60s ago
        when(jwtUtil.extractExpiration("expired.token")).thenReturn(pastDate);

        redisTokenService.blacklistAccessToken("expired.token");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void blacklistAccessToken_ExceptionThrown_ShouldNotThrow() {
        when(jwtUtil.extractExpiration("bad.token")).thenThrow(new RuntimeException("parse error"));

        assertDoesNotThrow(() -> redisTokenService.blacklistAccessToken("bad.token"));
    }

    @Test
    void isBlacklisted_ReturnsTrue() {
        when(redisTemplate.hasKey("blacklist:token")).thenReturn(true);

        assertTrue(redisTokenService.isBlacklisted("token"));
    }

    @Test
    void isBlacklisted_ReturnsFalse() {
        when(redisTemplate.hasKey("blacklist:token")).thenReturn(false);

        assertFalse(redisTokenService.isBlacklisted("token"));
    }

    // =========================================================================
    // Refresh Token
    // =========================================================================

    @Test
    void storeRefreshToken_Success() {
        when(jwtUtil.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        redisTokenService.storeRefreshToken("user@example.com", "rt.123");

        verify(valueOps).set(eq("refresh:user@example.com"), eq("rt.123"), any(Duration.class));
    }

    @Test
    void getRefreshToken_ReturnsToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refresh:user@example.com")).thenReturn("stored.token");

        String result = redisTokenService.getRefreshToken("user@example.com");
        assertEquals("stored.token", result);
    }

    @Test
    void getRefreshToken_ReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refresh:nobody@example.com")).thenReturn(null);

        assertNull(redisTokenService.getRefreshToken("nobody@example.com"));
    }

    @Test
    void deleteRefreshToken_Success() {
        redisTokenService.deleteRefreshToken("user@example.com");
        verify(redisTemplate).delete("refresh:user@example.com");
    }

    // =========================================================================
    // Reset Token
    // =========================================================================

    @Test
    void storeResetToken_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        redisTokenService.storeResetToken("reset-uuid", "user@example.com");

        verify(valueOps).set(eq("reset:reset-uuid"), eq("user@example.com"), eq(Duration.ofMinutes(15)));
    }

    @Test
    void getResetToken_ReturnsEmail() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("reset:token-abc")).thenReturn("test@example.com");

        assertEquals("test@example.com", redisTokenService.getResetToken("token-abc"));
    }

    @Test
    void deleteResetToken_Success() {
        redisTokenService.deleteResetToken("token-abc");
        verify(redisTemplate).delete("reset:token-abc");
    }

    // =========================================================================
    // OTP
    // =========================================================================

    @Test
    void storeOtp_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        redisTokenService.storeOtp("user@example.com", "123456");

        verify(valueOps).set(eq("otp:user@example.com"), eq("123456"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void getOtp_ReturnsOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:user@example.com")).thenReturn("654321");

        assertEquals("654321", redisTokenService.getOtp("user@example.com"));
    }

    @Test
    void getOtp_ReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:missing@example.com")).thenReturn(null);

        assertNull(redisTokenService.getOtp("missing@example.com"));
    }

    @Test
    void deleteOtp_Success() {
        redisTokenService.deleteOtp("user@example.com");
        verify(redisTemplate).delete("otp:user@example.com");
    }
}
