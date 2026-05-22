package com.connectsphere.auth.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.connectsphere.auth.service.RedisTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// JWT Filter — validates every request's access token.
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final RedisTokenService redisTokenService;
	private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String token = extractTokenFromCookie(request);

		// Fallback to Authorization header for backward compatibility
		if (token == null) {
			String authHeader = request.getHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				token = authHeader.substring(7);
			}
		}

		if (token != null) {
			// Check if the token is blacklisted
			if (redisTokenService.isBlacklisted(token)) {
				log.warn("Attempt to use a blacklisted token");
				filterChain.doFilter(request, response);
				return;
			}

			// Validate token
			if (jwtUtil.validateToken(token)) {
				String email = jwtUtil.extractEmail(token);
				String role = jwtUtil.extractRole(token);
				Long userId = jwtUtil.extractUserId(token);
				log.debug("JWT valid — email: {}, role: {}, userId: {}", email, role, userId);

				// Create authentication object
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,
						null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

				// Set authentication in context
				SecurityContextHolder.getContext().setAuthentication(authentication);
				// Forward userId as request attribute (used by downstream services)
				if (userId != null) {
					request.setAttribute("X-User-Id", userId);
				}
			} else {
				log.warn("Invalid or expired JWT token received");
			}
		}

		filterChain.doFilter(request, response);
	}

	private String extractTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("jwt".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}
}