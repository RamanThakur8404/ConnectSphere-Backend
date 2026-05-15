package com.connectsphere.admin_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final String JWT_SECRET = "c2VjcmV0a2V5c2VjcmV0a2V5c2VjcmV0a2V5MTIzNDU2";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", JWT_SECRET);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = createValidToken();

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithNullToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtUtil.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithEmptyToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtUtil.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void extractEmail_WithValidToken_ShouldReturnEmail() {
        // Given
        String email = "admin@test.com";
        String token = createTokenWithEmail(email);

        // When
        String extractedEmail = jwtUtil.extractEmail(token);

        // Then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    void extractEmail_WithInvalidToken_ShouldReturnNull() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        String extractedEmail = jwtUtil.extractEmail(invalidToken);

        // Then
        assertThat(extractedEmail).isNull();
    }

    @Test
    void extractRole_WithValidToken_ShouldReturnRole() {
        // Given
        String role = "ADMIN";
        String token = createTokenWithRole(role);

        // When
        String extractedRole = jwtUtil.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    void extractRole_WithInvalidToken_ShouldReturnNull() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        String extractedRole = jwtUtil.extractRole(invalidToken);

        // Then
        assertThat(extractedRole).isNull();
    }

    @Test
    void extractUserId_WithValidToken_ShouldReturnUserId() {
        // Given
        Long userId = 123L;
        String token = createTokenWithUserId(userId);

        // When
        Long extractedUserId = jwtUtil.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void extractUserId_WithInvalidToken_ShouldReturnNull() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Long extractedUserId = jwtUtil.extractUserId(invalidToken);

        // Then
        assertThat(extractedUserId).isNull();
    }

    // Helper methods to create test tokens
    private String createValidToken() {
        return Jwts.builder()
                .setSubject("test@test.com")
                .claim("role", "ADMIN")
                .claim("userId", 123L)
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
    }

    private String createTokenWithEmail(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", "ADMIN")
                .claim("userId", 123L)
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
    }

    private String createTokenWithRole(String role) {
        return Jwts.builder()
                .setSubject("test@test.com")
                .claim("role", role)
                .claim("userId", 123L)
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
    }

    private String createTokenWithUserId(Long userId) {
        return Jwts.builder()
                .setSubject("test@test.com")
                .claim("role", "ADMIN")
                .claim("userId", userId)
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, JWT_SECRET)
                .compact();
    }
}
