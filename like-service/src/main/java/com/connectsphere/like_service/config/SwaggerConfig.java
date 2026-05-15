package com.connectsphere.like_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI likeServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere — Like Service API")
                        .description("Polymorphic reaction system for posts and comments. " +
                                     "Supports 6 reaction types: LIKE, LOVE, HAHA, WOW, SAD, ANGRY.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConnectSphere")
                                .email("dev@connectsphere.com")));
    }
}
