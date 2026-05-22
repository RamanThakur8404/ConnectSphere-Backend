package com.connectsphere.media_service.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectsphere.media_service.exception.MediaServiceException;
import com.connectsphere.media_service.service.MediaStorageService;
import com.connectsphere.media_service.service.StoredMediaFile;
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

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// Cloudinary-backed implementation of MediaStorageService.
// Uploads files to Cloudinary CDN instead of the local filesystem.
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "connectsphere.media.storage-provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryMediaStorageService implements MediaStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryMediaStorageService.class);

    private final Cloudinary cloudinary;

    @Override
    public StoredMediaFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaServiceException("Uploaded file is empty", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        String publicId = "connectsphere/" + generatePublicId(originalFilename);
        String resourceType = resolveResourceType(file.getContentType());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", resourceType,
                    "overwrite", true
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            String storedId = (String) uploadResult.get("public_id");

            logger.info("File uploaded to Cloudinary — publicId={}, url={}", storedId, secureUrl);

            return new StoredMediaFile(storedId, secureUrl);
        } catch (Exception ex) {
            logger.error("Cloudinary upload failed for file={}", originalFilename, ex);
            throw new MediaServiceException("Failed to upload media to cloud storage", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }

    @Override
    public Resource loadAsResource(String storedFilename) {
        // For Cloudinary, files are served via CDN URL — build it from the public ID
        try {
            String url = cloudinary.url()
                    .secure(true)
                    .resourceType("auto")
                    .generate(storedFilename);

            if (url == null) {
                throw new MediaServiceException("Media file not found", HttpStatus.NOT_FOUND);
            }

            Resource resource = new UrlResource(URI.create(url));
            if (!resource.exists()) {
                throw new MediaServiceException("Media file not found", HttpStatus.NOT_FOUND);
            }
            return resource;
        } catch (MediaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MediaServiceException("Media file not found", HttpStatus.NOT_FOUND, ex);
        }
    }

    @Override
    public String resolveContentType(String storedFilename) {
        // Cloudinary handles content negotiation via its CDN, but we infer from the public ID
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

    @Override
    public void delete(String storedFilename) {
        try {
            cloudinary.uploader().destroy(storedFilename, ObjectUtils.asMap(
                    "resource_type", "auto"
            ));
            logger.info("File deleted from Cloudinary — publicId={}", storedFilename);
        } catch (IOException ex) {
            logger.warn("Failed to delete file from Cloudinary — publicId={}", storedFilename, ex);
        }
    }

    private String generatePublicId(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "upload" : originalFilename);
        String nameWithoutExt = StringUtils.stripFilenameExtension(cleaned);
        // Use UUID to avoid collisions + original name for readability
        return UUID.randomUUID() + "_" + (nameWithoutExt != null ? nameWithoutExt : "file");
    }

    private String resolveResourceType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) {
            return "video";
        }
        return "image";
    }
}
