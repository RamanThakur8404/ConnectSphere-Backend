package com.connectsphere.notification_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger / OpenAPI 3.0 configuration for the Notification Service.
@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	@Bean
	public OpenAPI notificationServiceOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("ConnectSphere — Notification Service API")
						.description("REST API for managing in-app notifications, bulk broadcasts, and email alerts. "
								+ "All endpoints require X-User-Id / X-User-Role headers injected by the API Gateway.")
						.version("2.0")
						.contact(new Contact().name("ConnectSphere Team").email("dev@connectsphere.com")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
						new SecurityScheme().name(SECURITY_SCHEME_NAME).type(SecurityScheme.Type.HTTP).scheme("bearer")
								.bearerFormat("JWT")
								.description("JWT access token issued by auth-service. "
										+ "The API Gateway validates the token and forwards "
										+ "X-User-Id and X-User-Role headers to this service.")));
	}
}
