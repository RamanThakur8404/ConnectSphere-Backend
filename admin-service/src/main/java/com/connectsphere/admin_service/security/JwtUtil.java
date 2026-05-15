package com.connectsphere.admin_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e);
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e);
        }
        return false;
    }

    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e);
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("Error extracting role from token: {}", e);
            return null;
        }
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e);
            return null;
        }
    }
}
