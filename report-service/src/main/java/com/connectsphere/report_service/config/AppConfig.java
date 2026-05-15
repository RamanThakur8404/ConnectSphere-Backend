package com.connectsphere.report_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// Application configuration class for common beans.
@Configuration
public class AppConfig {

	// RestTemplate bean used to call external APIs (e.g., Claude API).
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	// ObjectMapper bean for JSON processing. - Supports Java date/time - Ignores
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
}