package com.connectsphere.media_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.Url;
import com.connectsphere.media_service.exception.MediaServiceException;
import com.connectsphere.media_service.service.StoredMediaFile;

@ExtendWith(MockitoExtension.class)
class CloudinaryMediaStorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private Url url;

    private CloudinaryMediaStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new CloudinaryMediaStorageService(cloudinary);
    }

    @Test
    @DisplayName("store uploads image bytes and returns Cloudinary public id/url")
    void store_imageUpload_returnsStoredMediaFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "Holiday.JPG", "image/jpeg", "bytes".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/connectsphere/holiday.jpg",
                "public_id", "connectsphere/generated_holiday"));

        StoredMediaFile result = storageService.store(file);

        assertThat(result.storedFilename()).isEqualTo("connectsphere/generated_holiday");
        assertThat(result.publicUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/connectsphere/holiday.jpg");

        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(file.getBytes()), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue())
                .containsEntry("resource_type", "image")
                .containsEntry("overwrite", true);
        assertThat((String) optionsCaptor.getValue().get("public_id"))
                .startsWith("connectsphere/")
                .contains("_Holiday");
    }

    @Test
    @DisplayName("store uses video resource type for video uploads")
    void store_videoUpload_usesVideoResourceType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", "video".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/video/upload/connectsphere/clip.mp4",
                "public_id", "connectsphere/generated_clip"));

        storageService.store(file);

        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(file.getBytes()), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue()).containsEntry("resource_type", "video");
    }

    @Test
    @DisplayName("store rejects empty uploads")
    void store_emptyUpload_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(MediaServiceException.class)
                .hasMessage("Uploaded file is empty");
    }

    @Test
    @DisplayName("store wraps Cloudinary IO failures")
    void store_uploadFailure_wrapsException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "bytes".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("network down"));

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(MediaServiceException.class)
                .hasMessage("Failed to upload media to cloud storage")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("loadAsResource maps missing Cloudinary URL to not found")
    void loadAsResource_missingUrl_throwsNotFound() {
        when(cloudinary.url()).thenReturn(url);
        when(url.secure(true)).thenReturn(url);
        when(url.resourceType("auto")).thenReturn(url);
        when(url.generate("missing")).thenReturn(null);

        assertThatThrownBy(() -> storageService.loadAsResource("missing"))
                .isInstanceOf(MediaServiceException.class)
                .hasMessage("Media file not found");
    }

    @Test
    @DisplayName("resolveContentType infers common image and video types")
    void resolveContentType_knownExtensions_returnsExpectedTypes() {
        assertThat(storageService.resolveContentType("photo.jpeg")).isEqualTo("image/jpeg");
        assertThat(storageService.resolveContentType("image.PNG")).isEqualTo("image/png");
        assertThat(storageService.resolveContentType("asset.webp")).isEqualTo("image/webp");
        assertThat(storageService.resolveContentType("animation.gif")).isEqualTo("image/gif");
        assertThat(storageService.resolveContentType("bitmap.bmp")).isEqualTo("image/bmp");
        assertThat(storageService.resolveContentType("clip.mp4")).isEqualTo("video/mp4");
        assertThat(storageService.resolveContentType("no-extension")).isEqualTo("application/octet-stream");
        assertThat(storageService.resolveContentType("archive.bin")).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("delete suppresses Cloudinary IO failures")
    void delete_cloudinaryFailure_doesNotThrow() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("connectsphere/file"), anyMap())).thenThrow(new IOException("delete failed"));

        assertThatCode(() -> storageService.delete("connectsphere/file")).doesNotThrowAnyException();
    }
}
