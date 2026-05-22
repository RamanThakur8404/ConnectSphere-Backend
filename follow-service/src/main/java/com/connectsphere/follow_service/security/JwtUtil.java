package com.connectsphere.follow_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// Utility component for JSON Web Token (JWT) operations within the Follow-Service.
@Component
public class JwtUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);

    // Shared secret key loaded from application.properties. 
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Token expiry in milliseconds (default: 86400000 = 24 h). 
    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    // -------------------------------------------------------------------------
    // Key helper
    // -------------------------------------------------------------------------

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    // Extracts the subject claim (typically the user's email or ID) from the token.
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    // Extracts the expiry date from the token.
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    // Validates whether a given JWT is structurally sound, not expired,
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            LOGGER.warn("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            LOGGER.warn("JWT token is unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            LOGGER.warn("JWT token is malformed: {}", ex.getMessage());
        } catch (SecurityException ex) {
            LOGGER.warn("JWT signature validation failed: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(getSigningKey())
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }
}
