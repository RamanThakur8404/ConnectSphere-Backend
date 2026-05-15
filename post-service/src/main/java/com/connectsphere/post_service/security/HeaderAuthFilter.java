package com.connectsphere.post_service.security;

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

// HeaderAuthFilter intercepts requests coming from the API Gateway.
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HeaderAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // The Gateway forwards these headers after successful JWT validation
        String userIdStr = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                // We use the User ID as the Principal since downstream services
                // care more about the ID for foreign-key operations (like authorId)
                String role = (userRole != null && !userRole.isEmpty()) ? userRole : "USER";
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userIdStr, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authenticated request for userId: {} with role: {}", userIdStr, role);
            } catch (Exception ex) {
                logger.warn("Failed to parse user headers: {}", ex.getMessage());
            }
        } else {
            logger.debug("No X-User-Id header found. Request proceeds as unauthenticated.");
        }

        filterChain.doFilter(request, response);
    }
}
