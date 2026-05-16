package com.connectsphere.report_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.connectsphere.report_service.security.HeaderAuthenticationFilter;

import lombok.RequiredArgsConstructor;

// Security configuration for the Report-Service. Uses header-based
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final HeaderAuthenticationFilter headerAuthFilter;

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("Local username/password authentication is disabled");
		};
	}

	// Defines security rules for API endpoints.
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Public endpoints
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**")
						.permitAll()

						// Report creation (any logged-in user)
						.requestMatchers(HttpMethod.POST, "/api/v1/reports").authenticated()

						// Admin-only endpoints
						.requestMatchers("/api/v1/reports/stats").hasRole("ADMIN")
						.requestMatchers("/api/v1/reports/user/**").authenticated()

						// Admin or Moderator endpoints
						.requestMatchers("/api/v1/reports/queue").hasAnyRole("ADMIN", "MODERATOR")
						.requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyRole("ADMIN", "MODERATOR")
						.requestMatchers(HttpMethod.PUT, "/api/v1/reports/**").hasAnyRole("ADMIN", "MODERATOR")

						.anyRequest().authenticated())
				.addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
