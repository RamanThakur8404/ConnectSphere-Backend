package com.connectsphere.apigateway.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

class JwtUtilTest {

	private static final String SECRET = Base64.getEncoder()
			.encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));

	private JwtUtil jwtUtil;

	@BeforeEach
	void setUp() {
		jwtUtil = new JwtUtil();
		ReflectionTestUtils.setField(jwtUtil, "secretKey", SECRET);
	}

	@Test
	void validateAndExtractReturnsClaimsFromValidToken() {
		String token = createToken("42", "ADMIN", "admin@connectsphere.com");

		Claims claims = jwtUtil.validateAndExtract(token);

		assertThat(jwtUtil.extractUserId(claims)).isEqualTo("42");
		assertThat(jwtUtil.extractRole(claims)).isEqualTo("ADMIN");
		assertThat(jwtUtil.extractEmail(claims)).isEqualTo("admin@connectsphere.com");
	}

	@Test
	void extractUserIdReturnsNullWhenClaimMissing() {
		Claims claims = Jwts.claims();

		assertThat(jwtUtil.extractUserId(claims)).isNull();
	}

	private String createToken(String userId, String role, String subject) {
		Key key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
		return Jwts.builder().setSubject(subject).claim("userId", userId).claim("role", role)
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}
}
