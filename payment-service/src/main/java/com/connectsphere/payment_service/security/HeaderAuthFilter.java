package com.connectsphere.payment_service.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Extracts user details from gateway headers and sets SecurityContext.
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(HeaderAuthFilter.class);

	public static final String HEADER_USER_ID = "X-User-Id";
	public static final String HEADER_USER_ROLE = "X-User-Role";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String userIdHeader = request.getHeader(HEADER_USER_ID);
		String roleHeader = request.getHeader(HEADER_USER_ROLE);

		if (userIdHeader != null && !userIdHeader.isBlank() && roleHeader != null && !roleHeader.isBlank()) {

			try {
				Long userId = Long.parseLong(userIdHeader);

				String role = roleHeader.startsWith("ROLE_") ? roleHeader : "ROLE_" + roleHeader;

				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null,
						List.of(new SimpleGrantedAuthority(role)));

				SecurityContextHolder.getContext().setAuthentication(auth);
				log.debug("Authenticated | userId={}, role={}", userId, roleHeader);

			} catch (NumberFormatException e) {
				log.warn("Invalid userId header | value={}", userIdHeader);
			}
		}

		filterChain.doFilter(request, response);
	}
}