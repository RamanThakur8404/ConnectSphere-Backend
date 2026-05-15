package com.connectsphere.apigateway.util;

import java.security.Key;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// JWT utility for the API Gateway.
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    // Derives the HMAC-SHA signing key from a Base64-encoded secret string.
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Parses and validates the JWT in a single operation.
    public Claims validateAndExtract(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Extracts the userId from the custom "userId" claim.
    public String extractUserId(Claims claims) {
        Object uid = claims.get("userId");
        if (uid == null) return null;
        return uid.toString();
    }

    // Extracts the role from the custom "role" claim.
    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    // Extracts the user's email from the JWT subject claim.
    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }
}
