package com.connectsphere.media_service.service.impl;

import com.connectsphere.media_service.config.MediaUploadProperties;
import com.connectsphere.media_service.exception.MediaServiceException;
import com.connectsphere.media_service.service.MediaStorageService;
import com.connectsphere.media_service.service.StoredMediaFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "connectsphere.media.storage-provider", havingValue = "local")
public class LocalMediaStorageService implements MediaStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalMediaStorageService.class);
    private static final String DEFAULT_STORAGE_DIR = "uploads";

    private final MediaUploadProperties uploadProperties;

    private Path storageRoot;
    private String publicBaseUrl;

    @PostConstruct
    void init() {
        String configuredStorageDir = StringUtils.trimWhitespace(uploadProperties.getStorageDir());
        if (!StringUtils.hasText(configuredStorageDir)) {
            configuredStorageDir = DEFAULT_STORAGE_DIR;
        }

        publicBaseUrl = StringUtils.trimWhitespace(uploadProperties.getPublicBaseUrl());
        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new MediaServiceException("Public media base URL must be configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        storageRoot = Path.of(configuredStorageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
            logger.info("Media files will be stored under {}", storageRoot);
        } catch (IOException ex) {
            throw new MediaServiceException("Failed to initialize media storage", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }

    @Override
    public StoredMediaFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaServiceException("Uploaded file is empty", HttpStatus.BAD_REQUEST);
        }

        String storedFilename = generateStoredFilename(file.getOriginalFilename());
        Path targetPath = resolveSafePath(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return new StoredMediaFile(storedFilename, buildPublicUrl(storedFilename));
        } catch (IOException ex) {
            throw new MediaServiceException("Failed to store uploaded media", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }

    @Override
    public Resource loadAsResource(String storedFilename) {
        Path path = resolveSafePath(storedFilename);

        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new MediaServiceException("Media file not found", HttpStatus.NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new MediaServiceException("Media file not found", HttpStatus.NOT_FOUND, ex);
        }
    }

    @Override
    public String resolveContentType(String storedFilename) {
        Path path = resolveSafePath(storedFilename);
        try {
            String contentType = Files.probeContentType(path);
            if (StringUtils.hasText(contentType)) {
                return contentType;
            }
            return fallbackContentType(storedFilename);
        } catch (IOException ex) {
            return fallbackContentType(storedFilename);
        }
    }

    @Override
    public void delete(String storedFilename) {
        Path path = resolveSafePath(storedFilename);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            logger.warn("Failed to delete orphaned media file {}", path, ex);
        }
    }

    private String generateStoredFilename(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "upload" : originalFilename);
        String extension = StringUtils.getFilenameExtension(cleaned);
        if (StringUtils.hasText(extension)) {
            return UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID().toString();
    }

    private Path resolveSafePath(String filename) {
        String cleaned = StringUtils.cleanPath(filename);
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..")) {
            throw new MediaServiceException("Invalid media path", HttpStatus.BAD_REQUEST);
        }

        Path resolvedPath = storageRoot.resolve(cleaned).normalize();
        if (!resolvedPath.startsWith(storageRoot)) {
            throw new MediaServiceException("Invalid media path", HttpStatus.BAD_REQUEST);
        }
        return resolvedPath;
    }

    private String buildPublicUrl(String storedFilename) {
        String baseUrl = publicBaseUrl;
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/api/v1/media/files/"
                + UriUtils.encodePathSegment(storedFilename, StandardCharsets.UTF_8);
    }

    private String fallbackContentType(String storedFilename) {
        String extension = StringUtils.getFilenameExtension(storedFilename);
        if (extension == null) {
            return "application/octet-stream";
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }
}
