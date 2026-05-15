package com.connectsphere.comment_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger / OpenAPI 3 configuration for comment-service.
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI commentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere — Comment Service API")
                        .description("Handles threaded comments, replies, likes, and moderation for ConnectSphere.")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("gateway-auth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-User-Id")
                                        .description("User ID injected by the API Gateway (X-User-Id header)")));
    }
}
