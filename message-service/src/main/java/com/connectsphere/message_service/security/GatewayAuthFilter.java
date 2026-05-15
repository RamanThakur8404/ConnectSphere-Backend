package com.connectsphere.message_service.security;

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

// Reads X-User-Id and X-User-Role headers forwarded by the API Gateway.
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthFilter.class);
    public static final String HEADER_USER_ID = "X-User-Id";
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

                String principal = userId + ":" + role;
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                request.setAttribute(HEADER_USER_ID, userId);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[GatewayAuthFilter] Authenticated userId={} role={}", userId, role);

            } catch (NumberFormatException e) {
                log.warn("[GatewayAuthFilter] Invalid X-User-Id header: {}", userIdHeader);
            }
        }

        filterChain.doFilter(request, response);
    }
}
