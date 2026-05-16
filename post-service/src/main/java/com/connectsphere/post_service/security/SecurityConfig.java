package com.connectsphere.post_service.security;

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

import lombok.RequiredArgsConstructor;

// Stateless security configuration for post-service. Public GET endpoints are
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final HeaderAuthFilter headerAuthFilter;

	// Swagger UI, OpenAPI spec, actuator, and public feed endpoints 
	private static final String[] PUBLIC_GET_PATTERNS = { "/api/v1/posts/**", "/actuator/**",
			// Swagger / SpringDoc
			"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**" };

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("Local username/password authentication is disabled");
		};
	}

	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Public read-only endpoints
                    .requestMatchers("/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**",
                            "/actuator/**").permitAll()
                 // Public read-only post endpoints
                    .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                    // Internal counter endpoints — require authentication (called by other services with JWT)
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/like").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/unlike").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comment").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/uncomment").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/share-count").authenticated()
                    // All other requests (POST, PUT, DELETE) require authentication
                    .anyRequest().authenticated()
            )
            .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
