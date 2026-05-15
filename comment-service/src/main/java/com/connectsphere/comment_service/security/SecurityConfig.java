package com.connectsphere.comment_service.security;

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

// Stateless security configuration for comment-service.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthFilter gatewayAuthFilter;

    private static final String[] PUBLIC_GET_PATTERNS = {
            "/api/v1/comments/post/**",
            "/api/v1/comments/*/replies",
            "/api/v1/comments/*"
    };

    private static final String[] ALWAYS_PUBLIC = {
            "/actuator/**",
            "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html"
    };
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(ALWAYS_PUBLIC).permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATTERNS).permitAll()
                    .requestMatchers("/api/v1/comments/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
