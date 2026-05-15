package com.connectsphere.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

// Main Entry Point for ConnectSphere API Gateway.
@SpringBootApplication
@EnableDiscoveryClient // Why: Tells the Gateway to register itself with Eureka, and more importantly,
						// allows it to discover where the other downstream microservices are running.
public class ApiGatewayApplication {

	public static void main(String[] args) {
		// Note regarding WebFlux: Spring Cloud Gateway is built on Spring WebFlux
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	// Expose ObjectMapper as a bean so the filter can serialize error
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
