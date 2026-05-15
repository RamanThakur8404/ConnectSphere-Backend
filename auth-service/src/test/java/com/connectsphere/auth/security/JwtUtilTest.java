package com.connectsphere.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // The secret key needs to be long enough for HS256 (at least 256 bits / 32 characters)
    private final String dummySecret = "ConnectSphereSuperSecretKeyForTestingPurposeOnly12345";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(dummySecret);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiryMs", 86400000L); // 1 day
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiryMs", 604800000L); // 7 days
    }

    @Test
    void generateToken_ShouldReturnValidJwt() {
        String token = jwtUtil.generateToken("test@example.com", "USER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(2, token.length() - token.replace(".", "").length());
    }

    @Test
    void generateToken_WithUserId_ShouldReturnValidJwt() {
        String token = jwtUtil.generateToken("test@example.com", "USER", 42L);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken("test@example.com", "USER");

        String email = jwtUtil.extractEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void extractRole_ShouldReturnCorrectRole() {
        String token = jwtUtil.generateToken("admin@example.com", "ADMIN");

        String role = jwtUtil.extractRole(token);

        assertEquals("ADMIN", role);
    }

    @Test
    void extractUserId_ShouldReturnCorrectId() {
        String token = jwtUtil.generateToken("test@example.com", "USER", 99L);

        Long userId = jwtUtil.extractUserId(token);

        assertEquals(99L, userId);
    }

    @Test
    void extractUserId_WithoutUserId_ShouldReturnNull() {
        String token = jwtUtil.generateToken("test@example.com", "USER"); // no userId overload

        Long userId = jwtUtil.extractUserId(token);

        assertNull(userId);
    }

    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        String token = jwtUtil.generateToken("test@example.com", "USER");

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtUtil.generateToken("test@example.com", "USER");

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token";

        boolean isValid = jwtUtil.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void generateRefreshToken_ShouldReturnValidJwt() {
        String token = jwtUtil.generateRefreshToken("test@example.com", "USER", 1L);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
        assertEquals("USER", jwtUtil.extractRole(token));
        assertEquals(1L, jwtUtil.extractUserId(token));
    }

    @Test
    void getRefreshTokenExpiryMs_ShouldReturn7Days() {
        long expiryMs = jwtUtil.getRefreshTokenExpiryMs();
        assertEquals(604800000L, expiryMs);
    }

    @Test
    void generateRefreshToken_ShouldHaveLongerExpiry() {
        String accessToken = jwtUtil.generateToken("test@example.com", "USER", 1L);
        String refreshToken = jwtUtil.generateRefreshToken("test@example.com", "USER", 1L);

        Date accessExpiry = jwtUtil.extractExpiration(accessToken);
        Date refreshExpiry = jwtUtil.extractExpiration(refreshToken);

        assertTrue(refreshExpiry.after(accessExpiry));
    }
}