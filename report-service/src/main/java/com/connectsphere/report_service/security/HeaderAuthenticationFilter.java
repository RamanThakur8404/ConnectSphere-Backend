package com.connectsphere.report_service.security;

import java.io.IOException;
import java.util.Locale;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

// Filter that sets authentication using request headers.
@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID   = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain)
	        throws ServletException, IOException {

	    String userIdHeader = request.getHeader(HEADER_USER_ID);
	    String roleHeader = request.getHeader(HEADER_USER_ROLE);

	    if (userIdHeader != null && roleHeader != null) {
	        try {
	            String normalizedRole = roleHeader.startsWith("ROLE_")
	                    ? roleHeader
	                    : "ROLE_" + roleHeader.trim().toUpperCase(Locale.ROOT);
	            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(normalizedRole));
	            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
	                    Integer.parseInt(userIdHeader),
	                    null,
	                    authorities);

	            SecurityContextHolder.getContext().setAuthentication(auth);
	            log.debug("Auth context set: userId={} role={}", userIdHeader, normalizedRole);
	        } catch (NumberFormatException ex) {
	            SecurityContextHolder.clearContext();
	            log.warn("Ignoring authentication headers with invalid user id: {}", userIdHeader);
	        }
	    }

	    filterChain.doFilter(request, response);
	}
}
