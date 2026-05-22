package com.connectsphere.report_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Configuration for OpenAPI (Swagger) documentation. Swagger UI is available at
@Configuration
public class OpenApiConfig {

	// Defines API documentation details and security configuration.
	@Bean
	public OpenAPI reportServiceOpenApi() {
		return new OpenAPI()
				.info(new Info().title("ConnectSphere — Report Service API").version("2.0")
						.description("Content moderation report management with AI-powered analysis")
						.contact(new Contact().name("BridgeLabz ConnectSphere Team").email("dev@connectsphere.com")))
				.addSecurityItem(new SecurityRequirement().addList("X-User-Id"))
				.components(new Components().addSecuritySchemes("X-User-Id",
						new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER)
								.name("X-User-Id").description("User ID passed in request header")));
	}
}