package com.connectsphere.notification_service.security;

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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs",     "/v3/api-docs/**",
            "/swagger-resources/**", "/webjars/**"
    };

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
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(SWAGGER_PATHS).permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/v1/notifications/all").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/notifications/bulk").hasAnyRole("ADMIN", "MODERATOR")
                    .requestMatchers(HttpMethod.POST, "/api/v1/notifications/email").hasAnyRole("ADMIN", "MODERATOR")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
