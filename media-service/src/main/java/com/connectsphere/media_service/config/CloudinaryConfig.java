package com.connectsphere.media_service.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Provides a configured Cloudinary bean when cloudinary storage is enabled.
@Configuration
@ConditionalOnProperty(name = "connectsphere.media.storage-provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", normalizeCloudName(cloudName),
                "api_key", trimToEmpty(apiKey),
                "api_secret", trimToEmpty(apiSecret),
                "secure", true
        ));
    }

    private String normalizeCloudName(String value) {
        return trimToEmpty(value).toLowerCase();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
