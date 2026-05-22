package com.connectsphere.search_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// JWT authentication filter — executed once per request.
@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

	private static final String USER_ID_HEADER = "X-User-Id";
	private static final String USER_ROLE_HEADER = "X-User-Role";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String userId = request.getHeader(USER_ID_HEADER);
		String role = request.getHeader(USER_ROLE_HEADER);

		if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {
			// Normalise role — gateway sends "USER" but Spring Security needs "ROLE_USER"
			String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId,
					null, // no credentials needed
					List.of(new SimpleGrantedAuthority(authority)) // grants role-based access
			);

			SecurityContextHolder.getContext().setAuthentication(authentication);
			log.debug("Authenticated from gateway headers — userId: {}, role: {}", userId, role);
		} else {
			log.trace("No gateway headers present — request will be treated as anonymous");
		}

		filterChain.doFilter(request, response);
	}
}
