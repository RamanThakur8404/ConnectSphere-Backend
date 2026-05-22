package com.connectsphere.media_service.security;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.connectsphere.media_service.constants.LogMessages;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Authentication filter using headers from API Gateway. Gateway validates JWT
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

			// Build a Spring Security authentication object from trusted headers
			String principal = "user:" + userId;
			String authority = "ROLE_" + (role != null ? role.toUpperCase(Locale.ROOT) : "USER");

			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
					List.of(new SimpleGrantedAuthority(authority)));

			// Store userId as details so controllers can extract it easily
			auth.setDetails(userId);

			SecurityContextHolder.getContext().setAuthentication(auth);
			log.debug(LogMessages.HEADER_AUTH_SUCCESS, userId, role);

		} else if (userId == null) {
			// Swagger, actuator, and public GET endpoints are permitted without headers
			log.trace(LogMessages.HEADER_AUTH_MISSING, request.getRequestURI());
		}

		filterChain.doFilter(request, response);
	}
}
