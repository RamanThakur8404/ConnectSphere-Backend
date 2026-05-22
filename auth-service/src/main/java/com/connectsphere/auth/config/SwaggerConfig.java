package com.connectsphere.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Swagger / OpenAPI 3 configuration for the Auth Service.
@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	// Defines the OpenAPI metadata and global security scheme. The JWT bearer token
	@Bean
	public OpenAPI authServiceOpenAPI() {
		return new OpenAPI().info(apiInfo()).addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	// Builds API metadata shown in the Swagger UI header.
	private Info apiInfo() {
		return new Info().title("ConnectSphere — Auth Service API")
				.description("Authentication and user-management microservice for the ConnectSphere platform. "
						+ "Handles registration, JWT-based login/logout, token refresh, "
						+ "profile management, and admin operations.")
				.version("1.0.0").contact(new Contact().name("ConnectSphere Team").email("support@connectsphere.com"))
				.license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));
	}

	// Declares a Bearer-token (JWT) security scheme. This allows Swagger UI to pass
	private SecurityScheme jwtSecurityScheme() {
		return new SecurityScheme().name(SECURITY_SCHEME_NAME).type(SecurityScheme.Type.HTTP).scheme("bearer")
				.bearerFormat("JWT").description("Paste your JWT token here (without the 'Bearer ' prefix).");
	}

}
