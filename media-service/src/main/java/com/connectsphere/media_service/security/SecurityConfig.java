package com.connectsphere.media_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

// Security configuration for media-service.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

	private static final String[] SWAGGER_PATHS = { "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs",
			"/v3/api-docs/**", "/swagger-resources/**", "/webjars/**" };

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.ignoringRequestMatchers(SWAGGER_PATHS)
				.ignoringRequestMatchers("/actuator/**", "/api/v1/**"))
				.formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Swagger + Actuator — no auth needed
						.requestMatchers(SWAGGER_PATHS).permitAll().requestMatchers("/actuator/**").permitAll()

						// Public read: anyone can view media or stories
						.requestMatchers(HttpMethod.GET, "/api/v1/media/**", "/api/v1/stories/**").permitAll()

						// All write operations require authentication
						.anyRequest().authenticated())
				.addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
