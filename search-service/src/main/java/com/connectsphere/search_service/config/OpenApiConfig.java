package com.connectsphere.search_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// OpenAPI 3.0 configuration for the Search / Hashtag Service.
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI searchServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere — Search & Hashtag Service")
                        .description("""
                                Microservice responsible for:
                                - Indexing post content to extract hashtags
                                - Real-time keyword search across posts and users
                                - Trending hashtag computation
                                - PostHashtag many-to-many mapping management
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere Dev Team")
                                .email("dev@connectsphere.com")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token obtained from auth-service /auth/login")));
    }
}
