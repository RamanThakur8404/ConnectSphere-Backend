package com.connectsphere.like_service.config;

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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthFilter.class);
	static final String HEADER_USER_ID = "X-User-Id";
	static final String HEADER_USER_ROLE = "X-User-Role";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String userId = request.getHeader(HEADER_USER_ID);
		String role = request.getHeader(HEADER_USER_ROLE);

		if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
			// Build a lightweight authentication object — no password, just identity +
			// authority
			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, // principal =
																										// userId string
					null, // no credentials needed
					List.of(new SimpleGrantedAuthority("ROLE_" + role)) // e.g. ROLE_USER
			);
			SecurityContextHolder.getContext().setAuthentication(auth);
			log.debug("Gateway auth set — userId: {}, role: {}", userId, role);
		} else {
			log.debug("No gateway headers present — request proceeds unauthenticated: {}", request.getRequestURI());
		}

		filterChain.doFilter(request, response);
	}
}
