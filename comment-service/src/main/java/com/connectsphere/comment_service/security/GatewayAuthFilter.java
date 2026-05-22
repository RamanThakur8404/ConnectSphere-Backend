package com.connectsphere.comment_service.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// JWT authentication filter for comment-service. Validates Bearer tokens using
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(GatewayAuthFilter.class);
	// Header name forwarded by the API Gateway containing the authenticated user's
	public static final String HEADER_USER_ID = "X-User-Id";

	// Header name forwarded by the API Gateway containing the authenticated user's
	public static final String HEADER_USER_ROLE = "X-User-Role";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String userIdHeader = request.getHeader(HEADER_USER_ID);
		String roleHeader = request.getHeader(HEADER_USER_ROLE);

		if (userIdHeader != null && roleHeader != null) {
			try {
				Long userId = Long.parseLong(userIdHeader);
				String role = roleHeader.trim().toUpperCase(Locale.ROOT);

				// Build a principal string "userId:role" for downstream use
				String principal = userId + ":" + role;

				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
						List.of(new SimpleGrantedAuthority("ROLE_" + role)));

				// Store userId as a request attribute for easy access in controllers
				request.setAttribute(HEADER_USER_ID, userId);

				SecurityContextHolder.getContext().setAuthentication(auth);
				log.debug("[GatewayAuthFilter] Authenticated userId={} role={}", userId, role);

			} catch (NumberFormatException e) {
				log.warn("[GatewayAuthFilter] Invalid X-User-Id header value: {}", userIdHeader);
				// Don't set authentication — request proceeds as anonymous
			}
		}

		filterChain.doFilter(request, response);
	}
}
