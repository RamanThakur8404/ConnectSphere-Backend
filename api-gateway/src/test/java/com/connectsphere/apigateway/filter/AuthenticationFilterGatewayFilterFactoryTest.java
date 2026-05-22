package com.connectsphere.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.connectsphere.apigateway.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

class AuthenticationFilterGatewayFilterFactoryTest {

	private JwtUtil jwtUtil;
	@SuppressWarnings("unchecked")
	private ReactiveRedisTemplate<String, String> redisTemplate = mock(ReactiveRedisTemplate.class);
	private AuthenticationFilterGatewayFilterFactory factory;
	private GatewayFilter filter;

	@BeforeEach
	void setUp() {
		jwtUtil = mock(JwtUtil.class);
		factory = new AuthenticationFilterGatewayFilterFactory(jwtUtil, redisTemplate, new ObjectMapper());
		filter = factory.apply(new AuthenticationFilterGatewayFilterFactory.Config());
	}

	@Test
	void filterReturnsUnauthorizedWhenTokenMissing() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secure").build());
		GatewayFilterChain chain = mock(GatewayFilterChain.class);

		filter.filter(exchange, chain).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(exchange);
	}

	@Test
	void filterReturnsUnauthorizedWhenTokenInvalid() {
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/secure").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token").build());
		GatewayFilterChain chain = mock(GatewayFilterChain.class);
		when(jwtUtil.validateAndExtract("bad-token")).thenThrow(new IllegalArgumentException("broken"));

		filter.filter(exchange, chain).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(redisTemplate, never()).hasKey(anyString());
	}

	@Test
	void filterReturnsUnauthorizedWhenTokenBlacklisted() {
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/secure").header(HttpHeaders.AUTHORIZATION, "Bearer revoked").build());
		GatewayFilterChain chain = mock(GatewayFilterChain.class);
		Claims claims = Jwts.claims().setSubject("user@connectsphere.com");
		claims.put("userId", "99");
		claims.put("role", "ADMIN");
		when(jwtUtil.validateAndExtract("revoked")).thenReturn(claims);
		when(jwtUtil.extractUserId(claims)).thenReturn("99");
		when(jwtUtil.extractRole(claims)).thenReturn("ADMIN");
		when(jwtUtil.extractEmail(claims)).thenReturn("user@connectsphere.com");
		when(redisTemplate.hasKey("blacklist:revoked")).thenReturn(Mono.just(true));

		filter.filter(exchange, chain).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(exchange);
	}

	@Test
	void filterAddsHeadersAndContinuesWhenTokenValidFromAuthorizationHeader() {
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/secure").header(HttpHeaders.AUTHORIZATION, "Bearer live-token").build());
		Claims claims = Jwts.claims().setSubject("user@connectsphere.com");
		claims.put("userId", "15");
		claims.put("role", "MODERATOR");
		when(jwtUtil.validateAndExtract("live-token")).thenReturn(claims);
		when(jwtUtil.extractUserId(claims)).thenReturn("15");
		when(jwtUtil.extractRole(claims)).thenReturn("MODERATOR");
		when(jwtUtil.extractEmail(claims)).thenReturn("user@connectsphere.com");
		when(redisTemplate.hasKey("blacklist:live-token")).thenReturn(Mono.just(false));

		AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
		GatewayFilterChain chain = serverWebExchange -> {
			forwarded.set(serverWebExchange);
			return Mono.empty();
		};

		filter.filter(exchange, chain).block(Duration.ofSeconds(5));

		assertThat(forwarded.get()).isNotNull();
		assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("15");
		assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("MODERATOR");
		assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Email"))
				.isEqualTo("user@connectsphere.com");
	}

	@Test
	void filterUsesCookieTokenAndFailsOpenWhenRedisUnavailable() {
		HttpCookie cookie = new HttpCookie("jwt", "cookie-token");
		MockServerHttpRequest request = MockServerHttpRequest.get("/secure").cookie(cookie).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		Claims claims = Jwts.claims().setSubject("cookie@connectsphere.com");
		claims.put("userId", "7");
		when(jwtUtil.validateAndExtract("cookie-token")).thenReturn(claims);
		when(jwtUtil.extractUserId(claims)).thenReturn("7");
		when(jwtUtil.extractRole(claims)).thenReturn(null);
		when(jwtUtil.extractEmail(claims)).thenReturn("cookie@connectsphere.com");
		when(redisTemplate.hasKey("blacklist:cookie-token"))
				.thenReturn(Mono.error(new IllegalStateException("redis offline")));

		AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
		GatewayFilterChain chain = serverWebExchange -> {
			forwarded.set(serverWebExchange);
			return Mono.empty();
		};

		filter.filter(exchange, chain).block(Duration.ofSeconds(5));

		assertThat(forwarded.get()).isNotNull();
		assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("7");
		assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
		assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Email"))
				.isEqualTo("cookie@connectsphere.com");
	}
}
