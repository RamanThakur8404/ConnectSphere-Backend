package com.connectsphere.report_service.aspect;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filter that adds userId and requestId to MDC for logging.
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String MDC_USER_ID    = "userId";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userId    = request.getHeader("X-User-Id");
            String requestId = request.getHeader("X-Request-Id");

            MDC.put(MDC_USER_ID,    userId    != null ? userId    : "anonymous");
            MDC.put(MDC_REQUEST_ID, requestId != null ? requestId : UUID.randomUUID().toString());

            filterChain.doFilter(request, response);
        } finally {
            // Always clear MDC to avoid leaking context across thread-pool reuse.
            MDC.clear();
        }
    }
}
