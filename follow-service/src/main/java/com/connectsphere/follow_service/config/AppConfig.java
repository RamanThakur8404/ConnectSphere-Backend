package com.connectsphere.follow_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// General application configuration beans for the Follow-Service.
@Configuration
public class AppConfig {

    // Shared {@link RestTemplate} bean for outbound HTTP calls to other microservices.
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
