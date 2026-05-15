package com.connectsphere.admin_service.security;

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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthFilter.class);
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String role = request.getHeader(HEADER_USER_ROLE);
        String email = request.getHeader(HEADER_USER_EMAIL);

        if (userId != null && !userId.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            String normalizedRole = role == null || role.isBlank()
                    ? "ROLE_USER"
                    : role.startsWith("ROLE_")
                    ? role.toUpperCase(Locale.ROOT)
                    : "ROLE_" + role.toUpperCase(Locale.ROOT);

            String principal = (email != null && !email.isBlank()) ? email : userId;

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(normalizedRole)));

            authentication.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated admin-service request via gateway headers for userId={} role={}", userId, normalizedRole);
        }

        filterChain.doFilter(request, response);
    }
}
