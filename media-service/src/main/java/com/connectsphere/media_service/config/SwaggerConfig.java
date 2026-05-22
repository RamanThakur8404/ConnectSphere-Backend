package com.connectsphere.media_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI mediaServiceOpenAPI() {
		return new OpenAPI().info(new Info().title("ConnectSphere — Media Service API")
				.description("Handles media uploads, CDN URL storage, and 24-hour ephemeral stories.").version("1.0.0")
				.contact(new Contact().name("ConnectSphere Team").email("dev@connectsphere.com")))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
				.components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme().name("bearerAuth")
						.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
	}
}
