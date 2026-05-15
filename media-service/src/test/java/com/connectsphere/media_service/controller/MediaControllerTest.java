package com.connectsphere.media_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.media_service.constants.MediaTypes;
import com.connectsphere.media_service.config.MultipartUploadConfig;
import com.connectsphere.media_service.dto.MediaRequestDto;
import com.connectsphere.media_service.dto.MediaResponseDto;
import com.connectsphere.media_service.dto.StoryRequestDto;
import com.connectsphere.media_service.dto.StoryResponseDto;
import com.connectsphere.media_service.exception.GlobalExceptionHandler;
import com.connectsphere.media_service.exception.InvalidMediaTypeException;
import com.connectsphere.media_service.exception.MediaNotFoundException;
import com.connectsphere.media_service.exception.StoryNotFoundException;
import com.connectsphere.media_service.exception.UnauthorizedMediaAccessException;
import com.connectsphere.media_service.security.SecurityConfig;
import com.connectsphere.media_service.service.MediaService;
import com.connectsphere.media_service.service.MediaStorageService;
import com.connectsphere.media_service.service.StoredMediaFile;
import com.fasterxml.jackson.databind.ObjectMapper;

// Slice tests for {@link MediaController}.
@WebMvcTest(MediaController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, MultipartUploadConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MediaController – unit tests")
class MediaControllerTest {

    private static final String BASE = "/api/v1";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  MediaService mediaService;
    @MockBean  MediaStorageService mediaStorageService;

    // Reusable test fixtures

    private MediaRequestDto  validMediaRequest;
    private MediaResponseDto mediaResponse;
    private StoryRequestDto  validStoryRequest;
    private StoryResponseDto storyResponse;

    @BeforeEach
    void setUp() {
        // MediaRequestDto – all @NotNull / @NotBlank / @Positive fields populated
        validMediaRequest = MediaRequestDto.builder()
                .uploaderId(1)
                .mediaTypes(MediaTypes.IMAGE)
                .sizeKb(200L)
                .mimeType("image/jpeg")
                .linkedPostId(10)
                .build();

        mediaResponse = MediaResponseDto.builder()
                .mediaId(100)
                .uploaderId(1)
                .url("https://cdn.connectsphere.io/media/photo.jpg")
                .mediaTypes(MediaTypes.IMAGE)
                .sizeKb(200L)
                .mimeType("image/jpeg")
                .linkedPostId(10)
                .uploadedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        // StoryRequestDto – all @NotNull fields populated
        validStoryRequest = StoryRequestDto.builder()
                .authorId(2)
                .mediaUrl("https://cdn.connectsphere.io/media/story.jpg")
                .caption("Morning walk")
                .mediaTypes(MediaTypes.IMAGE)
                .build();

        storyResponse = StoryResponseDto.builder()
                .storyId(200)
                .authorId(2)
                .mediaUrl("https://cdn.connectsphere.io/media/story.jpg")
                .caption("Morning walk")
                .mediaTypes(MediaTypes.IMAGE)
                .viewsCount(0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        lenient().doAnswer(invocation -> {
            MockMultipartFile file = invocation.getArgument(0);
            String filename = file != null ? file.getOriginalFilename() : "upload.bin";
            return storedMediaFile(filename);
        }).when(mediaStorageService).store(any());
    }

    // =========================================================================
    // POST /api/v1/media/upload
    // =========================================================================

    @Nested
    @DisplayName("POST /media/upload")
    class UploadMediaTests {

        @Test
        @DisplayName("201 CREATED – valid file + metadata parts")
        void upload_validRequest_returns201WithBody() throws Exception {
            when(mediaService.uploadMedia(any(MediaRequestDto.class), anyString()))
                    .thenReturn(mediaResponse);

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .file(metadataPart(validMediaRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.mediaId").value(100))
                    .andExpect(jsonPath("$.data.uploaderId").value(1))
                    .andExpect(jsonPath("$.data.mimeType").value("image/jpeg"))
                    .andExpect(jsonPath("$.data.isDeleted").value(false));
        }

        @Test
        @DisplayName("201 CREATED – missing derived metadata is populated from uploaded file")
        void upload_missingDerivedMetadata_fieldsAreDerivedFromFile() throws Exception {
            MediaRequestDto partialRequest = MediaRequestDto.builder()
                    .uploaderId(1)
                    .linkedPostId(10)
                    .build();

            when(mediaService.uploadMedia(any(MediaRequestDto.class), anyString()))
                    .thenReturn(mediaResponse);

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .file(metadataPart(partialRequest)))
                    .andExpect(status().isCreated());

            ArgumentCaptor<MediaRequestDto> requestCaptor = ArgumentCaptor.forClass(MediaRequestDto.class);
            verify(mediaService).uploadMedia(requestCaptor.capture(), anyString());

            assertThat(requestCaptor.getValue().getMimeType()).isEqualTo("image/jpeg");
            assertThat(requestCaptor.getValue().getMediaTypes()).isEqualTo(MediaTypes.IMAGE);
            assertThat(requestCaptor.getValue().getSizeKb()).isPositive();
        }

        @Test
        @DisplayName("201 CREATED – uploaderId falls back to authenticated user when metadata part is omitted")
        void upload_missingMetadata_usesAuthenticatedUser() throws Exception {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("user:9", null);
            authentication.setDetails("9");

            when(mediaService.uploadMedia(any(MediaRequestDto.class), anyString()))
                    .thenReturn(mediaResponse);

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .principal(authentication))
                    .andExpect(status().isCreated());

            ArgumentCaptor<MediaRequestDto> requestCaptor = ArgumentCaptor.forClass(MediaRequestDto.class);
            verify(mediaService).uploadMedia(requestCaptor.capture(), anyString());

            assertThat(requestCaptor.getValue().getUploaderId()).isEqualTo(9);
            assertThat(requestCaptor.getValue().getMimeType()).isEqualTo("image/jpeg");
            assertThat(requestCaptor.getValue().getMediaTypes()).isEqualTo(MediaTypes.IMAGE);
        }

        @Test
        @DisplayName("Stored media URL is passed to service")
        void upload_storageUrlPassedToService() throws Exception {
            when(mediaService.uploadMedia(any(), anyString())).thenReturn(mediaResponse);
            when(mediaStorageService.store(any()))
                    .thenReturn(storedMediaFile("banner.jpg"));

            mockMvc.perform(multipart(BASE + "/media/upload")
                    .file(jpegFile("banner.jpg"))
                    .file(metadataPart(validMediaRequest)));

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(mediaService).uploadMedia(any(), urlCaptor.capture());
            assertThat(urlCaptor.getValue())
                    .isEqualTo("http://localhost:8080/api/v1/media/files/banner.jpg");
        }

        @Test
        @DisplayName("mediaService.uploadMedia is called exactly once")
        void upload_callsServiceExactlyOnce() throws Exception {
            when(mediaService.uploadMedia(any(), anyString())).thenReturn(mediaResponse);

            mockMvc.perform(multipart(BASE + "/media/upload")
                    .file(jpegFile("photo.jpg"))
                    .file(metadataPart(validMediaRequest)));

            verify(mediaService, times(1)).uploadMedia(any(), anyString());
        }

        @Test
        @DisplayName("Stored file is served back with its media type")
        void getStoredFile_returnsBinaryContent() throws Exception {
            when(mediaStorageService.loadAsResource("photo.jpg"))
                    .thenReturn(new ByteArrayResource("img".getBytes()));
            when(mediaStorageService.resolveContentType("photo.jpg"))
                    .thenReturn(MediaType.IMAGE_JPEG_VALUE);

            mockMvc.perform(get(BASE + "/media/files/photo.jpg"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(content().bytes("img".getBytes()));
        }

        @Test
        @DisplayName("400 BAD REQUEST – 'file' multipart part is absent")
        void upload_missingFilePart_returns400() throws Exception {
            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(metadataPart(validMediaRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 BAD REQUEST – 'metadata' multipart part is absent")
        void upload_missingMetadataPart_returns400() throws Exception {
            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 BAD REQUEST – uploaderId is null (@NotNull violation)")
        void upload_nullUploaderId_returns400WithFieldError() throws Exception {
            MediaRequestDto noUploader = MediaRequestDto.builder()
                    .mediaTypes(MediaTypes.IMAGE).sizeKb(100L).mimeType("image/jpeg").build();

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .file(metadataPart(noUploader)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.uploaderId")
                            .value("Uploader ID must not be null"));
        }

        @Test
        @DisplayName("201 CREATED – mediaType can be derived from the uploaded file")
        void upload_nullMediaType_isDerivedFromFile() throws Exception {
            MediaRequestDto noType = MediaRequestDto.builder()
                    .uploaderId(1).sizeKb(100L).mimeType("image/jpeg").build();

            when(mediaService.uploadMedia(any(MediaRequestDto.class), anyString()))
                    .thenReturn(mediaResponse);

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .file(metadataPart(noType)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("201 CREATED – mimeType can be derived from the uploaded file")
        void upload_blankMimeType_isDerivedFromFile() throws Exception {
            MediaRequestDto noMime = MediaRequestDto.builder()
                    .uploaderId(1).mediaTypes(MediaTypes.IMAGE).sizeKb(100L).mimeType("").build();

            when(mediaService.uploadMedia(any(MediaRequestDto.class), anyString()))
                    .thenReturn(mediaResponse);

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .file(metadataPart(noMime)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("201 CREATED – sizeKb is derived from the uploaded file")
        void upload_zeroSizeKb_isDerivedFromFile() throws Exception {
            MediaRequestDto zeroSize = MediaRequestDto.builder()
                    .uploaderId(1).mediaTypes(MediaTypes.IMAGE).sizeKb(0L).mimeType("image/jpeg")
                    .build();

            when(mediaService.uploadMedia(any(MediaRequestDto.class), anyString()))
                    .thenReturn(mediaResponse);

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .file(metadataPart(zeroSize)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("400 BAD REQUEST – service throws InvalidMediaTypeException")
        void upload_invalidMimeType_returns400FromGlobalHandler() throws Exception {
            when(mediaService.uploadMedia(any(), anyString()))
                    .thenThrow(new InvalidMediaTypeException("image/bmp"));

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.bmp"))
                            .file(metadataPart(validMediaRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("image/bmp")));
        }

        @Test
        @DisplayName("500 INTERNAL SERVER ERROR – service throws unexpected exception")
        void upload_serviceThrowsGeneric_returns500() throws Exception {
            when(mediaService.uploadMedia(any(), anyString()))
                    .thenThrow(new RuntimeException("S3 unreachable"));

            mockMvc.perform(multipart(BASE + "/media/upload")
                            .file(jpegFile("photo.jpg"))
                            .file(metadataPart(validMediaRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500));
        }
    }

    // =========================================================================
    // GET /api/v1/media/{mediaId}
    // =========================================================================

    @Nested
    @DisplayName("GET /media/{mediaId}")
    class GetMediaByIdTests {

        @Test
        @DisplayName("200 OK – returns media DTO for existing mediaId")
        void getById_exists_returns200() throws Exception {
            when(mediaService.getMediaById(100)).thenReturn(mediaResponse);

            mockMvc.perform(get(BASE + "/media/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mediaId").value(100))
                    .andExpect(jsonPath("$.data.uploaderId").value(1))
                    .andExpect(jsonPath("$.data.url").value("https://cdn.connectsphere.io/media/photo.jpg"))
                    .andExpect(jsonPath("$.data.mediaTypes").value("IMAGE"))
                    .andExpect(jsonPath("$.data.isDeleted").value(false));
        }

        @Test
        @DisplayName("404 NOT FOUND – service throws MediaNotFoundException; ApiResponse body returned")
        void getById_notFound_returns404WithApiResponse() throws Exception {
            when(mediaService.getMediaById(999)).thenThrow(new MediaNotFoundException(999));

            mockMvc.perform(get(BASE + "/media/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Media not found with id: 999"));
        }

        @Test
        @DisplayName("Correct mediaId is forwarded to service")
        void getById_forwardsIdToService() throws Exception {
            when(mediaService.getMediaById(42)).thenReturn(mediaResponse);

            mockMvc.perform(get(BASE + "/media/42"));

            verify(mediaService).getMediaById(42);
        }

        @Test
        @DisplayName("400 BAD REQUEST – non-numeric mediaId in path variable")
        void getById_nonNumericId_returns400() throws Exception {
            mockMvc.perform(get(BASE + "/media/abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("500 INTERNAL SERVER ERROR – service throws unexpected exception")
        void getById_serviceThrowsGeneric_returns500() throws Exception {
            when(mediaService.getMediaById(anyInt()))
                    .thenThrow(new RuntimeException("DB timeout"));

            mockMvc.perform(get(BASE + "/media/1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500));
        }
    }

    // =========================================================================
    // GET /api/v1/media/post/{postId}
    // =========================================================================

    @Nested
    @DisplayName("GET /media/post/{postId}")
    class GetMediaByPostTests {

        @Test
        @DisplayName("200 OK – returns single media record for a post")
        void getByPost_singleRecord_returns200() throws Exception {
            when(mediaService.getMediaByPost(10)).thenReturn(List.of(mediaResponse));

            mockMvc.perform(get(BASE + "/media/post/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].mediaId").value(100))
                    .andExpect(jsonPath("$.data[0].linkedPostId").value(10));
        }

        @Test
        @DisplayName("200 OK – returns empty array when post has no media")
        void getByPost_noMedia_returnsEmptyArray() throws Exception {
            when(mediaService.getMediaByPost(99)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/media/post/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("200 OK – returns multiple records for a post")
        void getByPost_multipleRecords_returnsAll() throws Exception {
            MediaResponseDto second = MediaResponseDto.builder()
                    .mediaId(101).uploaderId(1).linkedPostId(10)
                    .mediaTypes(MediaTypes.VIDEO).build();
            when(mediaService.getMediaByPost(10)).thenReturn(List.of(mediaResponse, second));

            mockMvc.perform(get(BASE + "/media/post/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[1].mediaTypes").value("VIDEO"));
        }

        @Test
        @DisplayName("Correct postId is forwarded to service")
        void getByPost_forwardsIdToService() throws Exception {
            when(mediaService.getMediaByPost(55)).thenReturn(List.of());

            mockMvc.perform(get(BASE + "/media/post/55"));

            verify(mediaService).getMediaByPost(55);
        }

        @Test
        @DisplayName("400 BAD REQUEST – non-numeric postId in path variable")
        void getByPost_nonNumericId_returns400() throws Exception {
            mockMvc.perform(get(BASE + "/media/post/xyz"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // DELETE /api/v1/media/{mediaId}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /media/{mediaId}")
    class DeleteMediaTests {

        @Test
        @DisplayName("204 NO CONTENT – successful soft-delete with empty body")
        void delete_exists_returns204() throws Exception {
            doNothing().when(mediaService).deleteMedia(100);

            mockMvc.perform(delete(BASE + "/media/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Media deleted successfully"));
        }

        @Test
        @DisplayName("404 NOT FOUND – service throws MediaNotFoundException; ApiResponse body returned")
        void delete_notFound_returns404WithApiResponse() throws Exception {
            doThrow(new MediaNotFoundException(999)).when(mediaService).deleteMedia(999);

            mockMvc.perform(delete(BASE + "/media/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Media not found with id: 999"));
        }

        @Test
        @DisplayName("403 FORBIDDEN – service throws UnauthorizedMediaAccessException")
        void delete_unauthorized_returns403() throws Exception {
            doThrow(new UnauthorizedMediaAccessException(5, 1))
                    .when(mediaService).deleteMedia(100);

            mockMvc.perform(delete(BASE + "/media/100"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(containsString("not authorized")));
        }

        @Test
        @DisplayName("mediaService.deleteMedia called exactly once with correct ID")
        void delete_callsServiceOnce() throws Exception {
            doNothing().when(mediaService).deleteMedia(100);

            mockMvc.perform(delete(BASE + "/media/100"));

            verify(mediaService, times(1)).deleteMedia(100);
        }

        @Test
        @DisplayName("400 BAD REQUEST – non-numeric mediaId in path variable")
        void delete_nonNumericId_returns400() throws Exception {
            mockMvc.perform(delete(BASE + "/media/xyz"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("500 INTERNAL SERVER ERROR – service throws unexpected exception")
        void delete_serviceThrowsGeneric_returns500() throws Exception {
            doThrow(new RuntimeException("DB down")).when(mediaService).deleteMedia(anyInt());

            mockMvc.perform(delete(BASE + "/media/100"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // POST /api/v1/stories
    // =========================================================================

    @Nested
    @DisplayName("POST /stories")
    class CreateStoryTests {

        @Test
        @DisplayName("201 CREATED – valid story request body with all fields")
        void create_valid_returns201WithBody() throws Exception {
            when(mediaService.createStory(any(StoryRequestDto.class))).thenReturn(storyResponse);

            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validStoryRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.storyId").value(200))
                    .andExpect(jsonPath("$.data.authorId").value(2))
                    .andExpect(jsonPath("$.data.caption").value("Morning walk"))
                    .andExpect(jsonPath("$.data.isActive").value(true))
                    .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
        }

        @Test
        @DisplayName("400 BAD REQUEST – authorId null (@NotNull); field error in ApiResponse.data")
        void create_nullAuthorId_returns400WithFieldError() throws Exception {
            StoryRequestDto dto = StoryRequestDto.builder()
                    .mediaUrl("https://cdn.connectsphere.io/media/story.jpg")
                    .mediaTypes(MediaTypes.IMAGE).build();

            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.authorId").value("Author ID must not be null"));
        }

        @Test
        @DisplayName("400 BAD REQUEST – mediaUrl null (@NotNull)")
        void create_nullMediaUrl_returns400WithFieldError() throws Exception {
            StoryRequestDto dto = StoryRequestDto.builder()
                    .authorId(2).mediaTypes(MediaTypes.IMAGE).build();

            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.mediaUrl").value("Media URL must not be null"));
        }

        @Test
        @DisplayName("400 BAD REQUEST – mediaType null (@NotNull)")
        void create_nullMediaType_returns400WithFieldError() throws Exception {
            StoryRequestDto dto = StoryRequestDto.builder()
                    .authorId(2).mediaUrl("https://cdn.connectsphere.io/media/story.jpg").build();

            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.mediaTypes").value("Media type must not be null"));
        }

        @Test
        @DisplayName("400 BAD REQUEST – caption exceeds 500 characters (@Size)")
        void create_captionTooLong_returns400() throws Exception {
            StoryRequestDto dto = StoryRequestDto.builder()
                    .authorId(2)
                    .mediaUrl("https://cdn.connectsphere.io/media/story.jpg")
                    .mediaTypes(MediaTypes.IMAGE)
                    .caption("x".repeat(501))
                    .build();

            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.caption")
                            .value("Caption must not exceed 500 characters"));
        }

        @Test
        @DisplayName("201 CREATED – caption at boundary (exactly 500 chars) is accepted")
        void create_captionAtBoundary500_returns201() throws Exception {
            when(mediaService.createStory(any())).thenReturn(storyResponse);

            StoryRequestDto dto = StoryRequestDto.builder()
                    .authorId(2)
                    .mediaUrl("https://cdn.connectsphere.io/media/story.jpg")
                    .mediaTypes(MediaTypes.IMAGE)
                    .caption("x".repeat(500))
                    .build();

            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("201 CREATED – optional caption field omitted is accepted")
        void create_noCaptionField_returns201() throws Exception {
            when(mediaService.createStory(any())).thenReturn(storyResponse);

            StoryRequestDto dto = StoryRequestDto.builder()
                    .authorId(2)
                    .mediaUrl("https://cdn.connectsphere.io/media/story.jpg")
                    .mediaTypes(MediaTypes.IMAGE)
                    .build();

            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("415 UNSUPPORTED MEDIA TYPE – Content-Type is text/plain")
        void create_wrongContentType_returns415() throws Exception {
            mockMvc.perform(post(BASE + "/stories")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("plain text"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("mediaService.createStory is called exactly once")
        void create_callsServiceOnce() throws Exception {
            when(mediaService.createStory(any())).thenReturn(storyResponse);

            mockMvc.perform(post(BASE + "/stories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validStoryRequest)));

            verify(mediaService, times(1)).createStory(any(StoryRequestDto.class));
        }
    }

    // =========================================================================
    // GET /api/v1/stories/active
    // =========================================================================

    @Nested
    @DisplayName("GET /stories/active")
    class GetActiveStoriesTests {

        @Test
        @DisplayName("200 OK – returns active stories for a list of authorIds")
        void getActive_validIds_returns200() throws Exception {
            when(mediaService.getActiveStories(List.of(1, 2, 3)))
                    .thenReturn(List.of(storyResponse));

            mockMvc.perform(get(BASE + "/stories/active")
                            .param("authorIds", "1", "2", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].storyId").value(200))
                    .andExpect(jsonPath("$.data[0].isActive").value(true));
        }

        @Test
        @DisplayName("200 OK – returns empty list when no active stories exist")
        void getActive_noStories_returnsEmptyList() throws Exception {
            when(mediaService.getActiveStories(anyList())).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/stories/active")
                            .param("authorIds", "5", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("200 OK – single authorId is accepted")
        void getActive_singleAuthorId_returns200() throws Exception {
            when(mediaService.getActiveStories(List.of(1))).thenReturn(List.of(storyResponse));

            mockMvc.perform(get(BASE + "/stories/active").param("authorIds", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("400 BAD REQUEST – required authorIds param is absent")
        void getActive_missingParam_returns400() throws Exception {
            mockMvc.perform(get(BASE + "/stories/active"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Correct authorIds list is forwarded to service (ArgumentCaptor)")
        void getActive_forwardsAuthorIdListToService() throws Exception {
            when(mediaService.getActiveStories(anyList())).thenReturn(List.of());

            mockMvc.perform(get(BASE + "/stories/active")
                    .param("authorIds", "7", "8"));

            ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
            verify(mediaService).getActiveStories(captor.capture());
            assertThat(captor.getValue()).containsExactly(7, 8);
        }

        @Test
        @DisplayName("500 INTERNAL SERVER ERROR – service throws unexpected exception")
        void getActive_serviceThrowsGeneric_returns500() throws Exception {
            when(mediaService.getActiveStories(anyList()))
                    .thenThrow(new RuntimeException("Cache miss"));

            mockMvc.perform(get(BASE + "/stories/active").param("authorIds", "1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500));
        }
    }

    // =========================================================================
    // GET /api/v1/stories/user/{authorId}
    // =========================================================================

    @Nested
    @DisplayName("GET /stories/user/{authorId}")
    class GetStoriesByUserTests {

        @Test
        @DisplayName("200 OK – returns stories for a valid authorId")
        void getByUser_valid_returns200() throws Exception {
            when(mediaService.getStoriesByUser(2)).thenReturn(List.of(storyResponse));

            mockMvc.perform(get(BASE + "/stories/user/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].authorId").value(2))
                    .andExpect(jsonPath("$.data[0].isActive").value(true));
        }

        @Test
        @DisplayName("200 OK – empty list when user has no stories")
        void getByUser_noStories_returnsEmpty() throws Exception {
            when(mediaService.getStoriesByUser(99)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/stories/user/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("200 OK – multiple stories returned for a user")
        void getByUser_multipleStories_returnsAll() throws Exception {
            StoryResponseDto second = StoryResponseDto.builder()
                    .storyId(201).authorId(2).isActive(true).build();
            when(mediaService.getStoriesByUser(2)).thenReturn(List.of(storyResponse, second));

            mockMvc.perform(get(BASE + "/stories/user/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("400 BAD REQUEST – non-numeric authorId in path variable")
        void getByUser_nonNumericId_returns400() throws Exception {
            mockMvc.perform(get(BASE + "/stories/user/abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Correct authorId is forwarded to service")
        void getByUser_forwardsIdToService() throws Exception {
            when(mediaService.getStoriesByUser(15)).thenReturn(List.of());

            mockMvc.perform(get(BASE + "/stories/user/15"));

            verify(mediaService).getStoriesByUser(15);
        }
    }

    // =========================================================================
    // POST /api/v1/stories/{storyId}/view
    // =========================================================================

    @Nested
    @DisplayName("POST /stories/{storyId}/view")
    class ViewStoryTests {

        @Test
        @DisplayName("200 OK – view recorded successfully with empty body")
        void view_valid_returns200() throws Exception {
            doNothing().when(mediaService).viewStory(200, 5);

            mockMvc.perform(post(BASE + "/stories/200/view")
                            .param("viewerId", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Story viewed"));
        }

        @Test
        @DisplayName("404 NOT FOUND – service throws StoryNotFoundException; ApiResponse returned")
        void view_storyNotFound_returns404() throws Exception {
            doThrow(new StoryNotFoundException(999)).when(mediaService).viewStory(999, 5);

            mockMvc.perform(post(BASE + "/stories/999/view")
                            .param("viewerId", "5"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message")
                            .value("Story not found or has expired with id: 999"));
        }

        @Test
        @DisplayName("400 BAD REQUEST – viewerId request param is absent")
        void view_missingViewerId_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/stories/200/view"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 BAD REQUEST – non-numeric storyId in path variable")
        void view_nonNumericStoryId_returns400() throws Exception {
            mockMvc.perform(post(BASE + "/stories/abc/view")
                            .param("viewerId", "5"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Correct storyId and viewerId are forwarded to service")
        void view_forwardsCorrectParams() throws Exception {
            doNothing().when(mediaService).viewStory(anyInt(), anyInt());

            mockMvc.perform(post(BASE + "/stories/200/view")
                    .param("viewerId", "7"));

            verify(mediaService).viewStory(200, 7);
        }

        @Test
        @DisplayName("mediaService.viewStory is called exactly once")
        void view_callsServiceOnce() throws Exception {
            doNothing().when(mediaService).viewStory(anyInt(), anyInt());

            mockMvc.perform(post(BASE + "/stories/200/view")
                    .param("viewerId", "5"));

            verify(mediaService, times(1)).viewStory(200, 5);
        }

        @Test
        @DisplayName("500 INTERNAL SERVER ERROR – service throws unexpected exception")
        void view_serviceThrowsGeneric_returns500() throws Exception {
            doThrow(new RuntimeException("Counter shard failure"))
                    .when(mediaService).viewStory(anyInt(), anyInt());

            mockMvc.perform(post(BASE + "/stories/200/view")
                            .param("viewerId", "5"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // =========================================================================
    // DELETE /api/v1/stories/{storyId}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /stories/{storyId}")
    class DeleteStoryTests {

        @Test
        @DisplayName("204 NO CONTENT – successful soft-delete with empty body")
        void delete_valid_returns204() throws Exception {
            doNothing().when(mediaService).deleteStory(200);

            mockMvc.perform(delete(BASE + "/stories/200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Story deleted"));
        }

        @Test
        @DisplayName("404 NOT FOUND – service throws StoryNotFoundException; ApiResponse returned")
        void delete_notFound_returns404WithApiResponse() throws Exception {
            doThrow(new StoryNotFoundException(999)).when(mediaService).deleteStory(999);

            mockMvc.perform(delete(BASE + "/stories/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message")
                            .value("Story not found or has expired with id: 999"));
        }

        @Test
        @DisplayName("403 FORBIDDEN – service throws UnauthorizedMediaAccessException")
        void delete_unauthorized_returns403() throws Exception {
            doThrow(new UnauthorizedMediaAccessException(5, 2))
                    .when(mediaService).deleteStory(200);

            mockMvc.perform(delete(BASE + "/stories/200"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(containsString("not authorized")));
        }

        @Test
        @DisplayName("mediaService.deleteStory called exactly once with correct ID")
        void delete_callsServiceOnce() throws Exception {
            doNothing().when(mediaService).deleteStory(200);

            mockMvc.perform(delete(BASE + "/stories/200"));

            verify(mediaService, times(1)).deleteStory(200);
        }

        @Test
        @DisplayName("400 BAD REQUEST – non-numeric storyId in path variable")
        void delete_nonNumericId_returns400() throws Exception {
            mockMvc.perform(delete(BASE + "/stories/xyz"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("500 INTERNAL SERVER ERROR – service throws unexpected exception")
        void delete_serviceThrowsGeneric_returns500() throws Exception {
            doThrow(new RuntimeException("DB error")).when(mediaService).deleteStory(anyInt());

            mockMvc.perform(delete(BASE + "/stories/200"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500));
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    // Builds a JPEG {@link MockMultipartFile} for the "file" part. 
    private MockMultipartFile jpegFile(String filename) {
        return new MockMultipartFile(
                "file", filename, org.springframework.http.MediaType.IMAGE_JPEG_VALUE,
                ("fake-jpeg-bytes-" + filename).getBytes());
    }

    // Serialises a {@link MediaRequestDto} into a JSON {@link MockMultipartFile}
    private MockMultipartFile metadataPart(MediaRequestDto dto) throws Exception {
        return new MockMultipartFile(
                "metadata", "",
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto));
    }

    private StoredMediaFile storedMediaFile(String filename) {
        return new StoredMediaFile(filename, "http://localhost:8080/api/v1/media/files/" + filename);
    }
}
