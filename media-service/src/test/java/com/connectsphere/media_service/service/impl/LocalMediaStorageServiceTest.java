package com.connectsphere.media_service.service.impl;

import com.connectsphere.media_service.config.MediaUploadProperties;
import com.connectsphere.media_service.exception.MediaServiceException;
import com.connectsphere.media_service.service.StoredMediaFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class LocalMediaStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalMediaStorageService storageService;

    @BeforeEach
    void setUp() {
        MediaUploadProperties properties = new MediaUploadProperties();
        properties.setStorageDir(tempDir.toString());
        properties.setPublicBaseUrl("http://localhost:8080");

        storageService = new LocalMediaStorageService(properties);
        storageService.init();
    }

    @Test
    @DisplayName("store writes file to disk and returns public URL")
    void store_writesFileAndReturnsUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.JPG", "image/jpeg", "image-bytes".getBytes());

        StoredMediaFile storedMediaFile = storageService.store(file);

        assertThat(storedMediaFile.publicUrl())
                .startsWith("http://localhost:8080/api/v1/media/files/");
        assertThat(storedMediaFile.storedFilename()).endsWith(".jpg");
        assertThat(Files.exists(tempDir.resolve(storedMediaFile.storedFilename()))).isTrue();
    }

    @Test
    @DisplayName("loadAsResource returns readable stored file")
    void loadAsResource_returnsStoredFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", "video-bytes".getBytes());

        StoredMediaFile storedMediaFile = storageService.store(file);
        Resource resource = storageService.loadAsResource(storedMediaFile.storedFilename());

        assertThat(resource.exists()).isTrue();
        assertThat(storageService.resolveContentType(storedMediaFile.storedFilename()))
                .isEqualTo("video/mp4");
    }

    @Test
    @DisplayName("invalid path traversal is rejected")
    void loadAsResource_pathTraversalRejected() {
        assertThatThrownBy(() -> storageService.loadAsResource("../secret.txt"))
                .isInstanceOf(MediaServiceException.class)
                .hasMessage("Invalid media path");
    }

    @Test
    @DisplayName("store rejects empty uploads")
    void store_emptyFileRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(MediaServiceException.class)
                .extracting("status")
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("loadAsResource returns not found when file does not exist")
    void loadAsResource_missingFileRejected() {
        assertThatThrownBy(() -> storageService.loadAsResource("missing.jpg"))
                .isInstanceOf(MediaServiceException.class)
                .extracting("status")
                .isEqualTo(NOT_FOUND);
    }

    @Test
    @DisplayName("resolveContentType falls back to octet-stream for unknown extensions")
    void resolveContentType_unknownExtensionFallsBack() {
        assertThat(storageService.resolveContentType("archive.unknown"))
                .isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("delete removes stored file when present")
    void delete_existingFileRemovesIt() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cleanup.png", "image/png", "png-bytes".getBytes());

        StoredMediaFile storedMediaFile = storageService.store(file);
        Path storedPath = tempDir.resolve(storedMediaFile.storedFilename());

        assertThat(Files.exists(storedPath)).isTrue();

        storageService.delete(storedMediaFile.storedFilename());

        assertThat(Files.exists(storedPath)).isFalse();
    }

    @Test
    @DisplayName("init rejects blank public base URL")
    void init_blankPublicBaseUrlRejected() {
        MediaUploadProperties properties = new MediaUploadProperties();
        properties.setStorageDir(tempDir.toString());
        properties.setPublicBaseUrl("   ");

        LocalMediaStorageService misconfiguredService = new LocalMediaStorageService(properties);

        assertThatThrownBy(misconfiguredService::init)
                .isInstanceOf(MediaServiceException.class)
                .extracting("status")
                .isEqualTo(INTERNAL_SERVER_ERROR);
    }
}
