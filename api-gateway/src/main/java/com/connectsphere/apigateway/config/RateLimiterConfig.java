package com.connectsphere.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Configures the Redis-backed rate limiter for Spring Cloud Gateway.
 *
 * KeyResolver strategy: rate-limit by client IP address so that a single
 * bad actor cannot exhaust the budget for all users.
 *
 * The actual replenishment rate and burst capacity are configured per-route
 * in application.yml using the RequestRateLimiter GatewayFilter.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the rate-limit bucket key from the client's real IP address.
     * Falls back to "unknown" if no remote address is available (should not
     * happen behind a properly configured reverse proxy).
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
