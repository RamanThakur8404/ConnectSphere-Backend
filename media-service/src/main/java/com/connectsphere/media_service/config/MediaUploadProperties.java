package com.connectsphere.media_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "connectsphere.media.upload")
public class MediaUploadProperties {

    private DataSize maxFileSize = DataSize.ofMegabytes(50);

    private DataSize maxRequestSize = DataSize.ofMegabytes(60);

    private String storageDir = "uploads";

    private String publicBaseUrl = "http://localhost:8080";
}
