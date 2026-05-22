package com.connectsphere.apigateway.filter;

import com.connectsphere.apigateway.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Gateway-level JWT authentication filter.
@Component

public class AuthenticationFilterGatewayFilterFactory
		extends AbstractGatewayFilterFactory<AuthenticationFilterGatewayFilterFactory.Config> {

	private final JwtUtil jwtUtil;
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilterGatewayFilterFactory.class);

	// Redis key prefix for blacklisted tokens — must match auth-service convention
	private static final String BLACKLIST_PREFIX = "blacklist:";

	public AuthenticationFilterGatewayFilterFactory(JwtUtil jwtUtil,
			ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
		super(Config.class);
		this.jwtUtil = jwtUtil;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {

			// 1. Extract from HttpOnly Cookie first, fallback to Authorization Header
			org.springframework.http.HttpCookie cookie = exchange.getRequest().getCookies().getFirst("jwt");
			String token = null;

			if (cookie != null) {
				token = cookie.getValue();
			} else {
				String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
				if (authHeader != null && authHeader.startsWith("Bearer ")) {
					token = authHeader.substring(7);
				}
			}

			if (token == null || token.isEmpty()) {
                log.error("AuthenticationFilter: Token is null or empty. Did the browser send the 'jwt' cookie?");
				return onError(exchange, "MISSING_TOKEN", "Missing authorization token", HttpStatus.UNAUTHORIZED);
			}

			io.jsonwebtoken.Claims claims;
			try {
				// 2. Attempt to parse and validate token (Single-parse architecture)
				claims = jwtUtil.validateAndExtract(token);
			} catch (Exception e) {
				// Token is expired, malformed, or signatures don't match
                log.error("AuthenticationFilter: Token validation failed: {}", e.getMessage());
				return onError(exchange, "INVALID_TOKEN", "Unauthorized access to application",
						HttpStatus.UNAUTHORIZED);
			}

			// 3. Check Redis blacklist (handles logout)
			final String finalToken = token;
			String blacklistKey = BLACKLIST_PREFIX + token;

			return redisTemplate.hasKey(blacklistKey)
			        .onErrorResume(e -> {
			             log.error("Redis connection failed during token verification in API Gateway: {}", e.getMessage(), e);
			             // Fail-open strategy for Redis unavailability to prevent blocking valid logins
			             return Mono.just(false);
			        })
			        .flatMap(isBlacklisted -> {
				if (Boolean.TRUE.equals(isBlacklisted)) {
					return onError(exchange, "TOKEN_REVOKED", "This token has been invalidated. Please log in again.",
							HttpStatus.UNAUTHORIZED);
				}

				// 4. Extract claims from parsed token
				String userId = jwtUtil.extractUserId(claims);
				String role = jwtUtil.extractRole(claims);
				String email = jwtUtil.extractEmail(claims);

				// 5. Mutate request — add trusted internal headers
				ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
						.header("X-User-Id", userId != null ? userId : "")
						.header("X-User-Role", role != null ? role : "USER")
						.header("X-User-Email", email != null ? email : "")
						.build();

				return chain.filter(exchange.mutate().request(mutatedRequest).build());
			});
		};

	}

	// Helper to return consistent error responses without crashing the application.
	private Mono<Void> onError(ServerWebExchange exchange, String errorCode, String message, HttpStatus status) {
		var response = exchange.getResponse();
		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> errorBody = Map.of("error", errorCode, "message", message, "status", status.value(),
				"timestamp", LocalDateTime.now().toString());

		try {
			byte[] bytes = objectMapper.writeValueAsBytes(errorBody);
			var buffer = response.bufferFactory().wrap(bytes);
			return response.writeWith(Mono.just(buffer));
		} catch (JsonProcessingException e) {
			// Fallback — at minimum set the status
			return response.setComplete();
		}
	}

	public static class Config {
		// Add per-route config here if needed (e.g., skip blacklist check for certain
		// routes)
	}
}
