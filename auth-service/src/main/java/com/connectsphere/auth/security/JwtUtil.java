package com.connectsphere.auth.security;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private final String secretKey;

    @Value("${jwt.access-token-expiry-ms:86400000}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

    public JwtUtil(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    // -----------------------------------------------------------------------
    // Token Generation
    // -----------------------------------------------------------------------

    // Generates a short-lived access token (24 h).
    public String generateToken(String email, String role) {
        return buildToken(email, role, null, accessTokenExpiryMs);
    }

    // Generates a short-lived access token with userId embedded.
    public String generateToken(String email, String role, Long userId) {
        return buildToken(email, role, userId, accessTokenExpiryMs);
    }

    // Generates a long-lived refresh token (7 days).
    public String generateRefreshToken(String email, String role, Long userId) {
        return buildToken(email, role, userId, refreshTokenExpiryMs);
    }

    private String buildToken(String email, String role, Long userId, long expiryMs) {
        var builder = Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs));

        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder.signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
    }

    // -----------------------------------------------------------------------
    // Token Extraction
    // -----------------------------------------------------------------------

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    public Long extractUserId(String token) {
        Object uid = extractAllClaims(token).get("userId");
        if (uid == null)
            return null;
        if (uid instanceof Integer)
            return ((Integer) uid).longValue();
        if (uid instanceof Long)
            return (Long) uid;
        return Long.valueOf(uid.toString());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // -----------------------------------------------------------------------
    // Token Validation
    // -----------------------------------------------------------------------

    // Returns true if the token is structurally valid and not expired.
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    // Derives the HMAC-SHA signing key from a Base64-encoded secret string.
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
