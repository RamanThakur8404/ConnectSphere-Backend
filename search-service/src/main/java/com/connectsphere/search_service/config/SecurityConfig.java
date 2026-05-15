package com.connectsphere.search_service.config;

import com.connectsphere.search_service.security.GatewayHeaderAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Spring Security configuration for the Search / Hashtag Service.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

	// Paths that are fully public — no authentication required. 
	private static final String[] PUBLIC_GET_PATHS = { "/api/v1/search/posts", "/api/v1/search/users",
			"/api/v1/hashtags", "/api/v1/hashtags/trending", "/api/v1/hashtags/{tag}/posts",
			"/api/v1/hashtags/{tag}/count", "/api/v1/posts/{postId}/hashtags" };

	// Swagger / OpenAPI UI endpoints — always public. 
	private static final String[] SWAGGER_PATHS = { "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**",
			"/v3/api-docs/**" };

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
						.requestMatchers(SWAGGER_PATHS).permitAll()
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/actuator/**").authenticated()
						.requestMatchers(HttpMethod.DELETE, "/api/v1/search/index/**").hasAuthority("ROLE_ADMIN")
						.anyRequest().authenticated())
				.addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
