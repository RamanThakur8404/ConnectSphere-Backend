package com.connectsphere.notification_service.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.connectsphere.notification_service.constants.LogMessages;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// JWT authentication filter for notification-service.
@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthFilter.class);
	// Header injected by the API Gateway after JWT validation. 
	public static final String HEADER_USER_ID = "X-User-Id";
	public static final String HEADER_USER_ROLE = "X-User-Role";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String userId = request.getHeader(HEADER_USER_ID);
		String role = request.getHeader(HEADER_USER_ROLE);

		if (userId != null && !userId.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {

			// Build a Spring Security authentication object from the trusted headers
			String principal = "user:" + userId;
			String authority = "ROLE_" + (role != null ? role.toUpperCase() : "USER");

			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
					List.of(new SimpleGrantedAuthority(authority)));

			// Store userId as details so controllers can extract it without re-parsing
			auth.setDetails(userId);

			SecurityContextHolder.getContext().setAuthentication(auth);
			log.debug(LogMessages.HEADER_AUTH_SUCCESS, userId, role);

		} else if (userId == null) {
			// Not all paths require auth — Swagger and actuator are permitted without
			// headers
			log.trace(LogMessages.HEADER_AUTH_MISSING, request.getRequestURI());
		}

		filterChain.doFilter(request, response);
	}
}
