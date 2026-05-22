package com.connectsphere.media_service.config;

import jakarta.servlet.MultipartConfigElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MediaUploadProperties.class)
public class MultipartUploadConfig {

    private static final Logger logger = LoggerFactory.getLogger(MultipartUploadConfig.class);

    @Bean
    MultipartConfigElement multipartConfigElement(MediaUploadProperties uploadProperties) {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(uploadProperties.getMaxFileSize());
        factory.setMaxRequestSize(uploadProperties.getMaxRequestSize());
        return factory.createMultipartConfig();
    }

    @Bean
    ApplicationRunner multipartLimitLogger(MediaUploadProperties uploadProperties) {
        return args -> logger.info(
                "Multipart upload limits configured: max-file-size={}, max-request-size={}",
                format(uploadProperties.getMaxFileSize()),
                format(uploadProperties.getMaxRequestSize()));
    }

    private String format(DataSize size) {
        return size.toMegabytes() + "MB";
    }
}
