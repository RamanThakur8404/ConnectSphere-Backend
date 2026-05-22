package com.connectsphere.media_service.controller;

import com.connectsphere.media_service.dto.MediaRequestDto;
import com.connectsphere.media_service.dto.MediaResponseDto;
import com.connectsphere.media_service.dto.StoryRequestDto;
import com.connectsphere.media_service.dto.StoryResponseDto;
import com.connectsphere.media_service.dto.ApiResponse;
import com.connectsphere.media_service.constants.MediaTypes;
import com.connectsphere.media_service.exception.InvalidMediaTypeException;
import com.connectsphere.media_service.service.MediaService;
import com.connectsphere.media_service.service.MediaStorageService;
import com.connectsphere.media_service.service.StoredMediaFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

//
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MediaController {

    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);

    private final MediaService mediaService;
    private final MediaStorageService mediaStorageService;
    private final SmartValidator validator;

    // Media endpoints

    // Uploads a media file and persists its metadata.
    @PostMapping("/media/upload")
    public ResponseEntity<ApiResponse<MediaResponseDto>> uploadMedia(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "metadata", required = false) MediaRequestDto dto,
            Authentication authentication) throws BindException {

        MediaRequestDto uploadRequest = dto != null ? dto : new MediaRequestDto();
        populateDerivedUploadFields(uploadRequest, file, authentication);
        validateUploadRequest(uploadRequest);
        StoredMediaFile storedMediaFile = mediaStorageService.store(file);

        String originalFileName = file.getOriginalFilename();
        String safeFileName = (originalFileName == null || originalFileName.isBlank())
                ? "uploaded-file"
                : originalFileName;

        logger.info("Received upload request from uploaderId={}, fileName={}",
                uploadRequest.getUploaderId(), safeFileName);

        try {
            MediaResponseDto response = mediaService.uploadMedia(uploadRequest, storedMediaFile.publicUrl());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Media uploaded successfully", response));
        } catch (RuntimeException ex) {
            mediaStorageService.delete(storedMediaFile.storedFilename());
            throw ex;
        }
    }

    @GetMapping("/media/files/{filename:.+}")
    public ResponseEntity<Resource> getMediaFile(@PathVariable String filename) {
        Resource resource = mediaStorageService.loadAsResource(filename);
        String contentType = mediaStorageService.resolveContentType(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // Retrieves a single media record by its ID.
    @GetMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<MediaResponseDto>> getMediaById(@PathVariable int mediaId) {
        logger.debug("GET /media/{}", mediaId);
        MediaResponseDto response = mediaService.getMediaById(mediaId);
        return ResponseEntity.ok(ApiResponse.success("Media retrieved", response));
    }

    // Retrieves all media records linked to a specific post.
    @GetMapping("/media/post/{postId}")
    public ResponseEntity<ApiResponse<List<MediaResponseDto>>> getMediaByPost(@PathVariable int postId) {
        logger.debug("GET /media/post/{}", postId);
        List<MediaResponseDto> response = mediaService.getMediaByPost(postId);
        return ResponseEntity.ok(ApiResponse.success("Post media retrieved", response));
    }

    // Soft-deletes a media record. The physical row and CDN file are retained.
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable int mediaId) {
        logger.info("DELETE /media/{}", mediaId);
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.success("Media deleted successfully", null));
    }

    // Story endpoints

    // Creates a new 24-hour story for the authenticated user.
    @PostMapping("/stories")
    public ResponseEntity<ApiResponse<StoryResponseDto>> createStory(@Valid @RequestBody StoryRequestDto dto) {
        logger.info("Creating story for authorId={}", dto.getAuthorId());
        StoryResponseDto response = mediaService.createStory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Story created successfully", response));
    }

    // Returns all currently active stories from a given list of user IDs.
    @GetMapping("/stories/active")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getActiveStories(
            @RequestParam List<Integer> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            logger.debug("GET /stories/active with no authorIds supplied");
            return ResponseEntity.ok(ApiResponse.success("Active stories retrieved", List.of()));
        }

        logger.debug("GET /stories/active for {} authors", authorIds.size());
        List<StoryResponseDto> response = mediaService.getActiveStories(authorIds);
        return ResponseEntity.ok(ApiResponse.success("Active stories retrieved", response));
    }

    // Returns all active stories created by a specific user.
    @GetMapping("/stories/user/{authorId}")
    public ResponseEntity<ApiResponse<List<StoryResponseDto>>> getStoriesByUser(@PathVariable int authorId) {
        logger.debug("GET /stories/user/{}", authorId);
        List<StoryResponseDto> response = mediaService.getStoriesByUser(authorId);
        return ResponseEntity.ok(ApiResponse.success("User stories retrieved", response));
    }

    // Records a story view from a specific user and increments the story's view counter.
    @PostMapping("/stories/{storyId}/view")
    public ResponseEntity<ApiResponse<Void>> viewStory(
            @PathVariable int storyId,
            @RequestParam int viewerId) {
        logger.info("User viewerId={} viewed storyId={}", viewerId, storyId);
        mediaService.viewStory(storyId, viewerId);
        return ResponseEntity.ok(ApiResponse.success("Story viewed", null));
    }

    // Soft-deletes a story before its natural 24-hour expiry.
    @DeleteMapping("/stories/{storyId}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable int storyId) {
        logger.info("DELETE /stories/{}", storyId);
        mediaService.deleteStory(storyId);
        return ResponseEntity.ok(ApiResponse.success("Story deleted", null));
    }

    private void populateDerivedUploadFields(
            MediaRequestDto dto,
            MultipartFile file,
            Authentication authentication) {

        if (dto.getUploaderId() == null) {
            dto.setUploaderId(resolveAuthenticatedUserId(authentication));
        }

        String mimeType = resolveMimeType(file, dto.getMimeType());
        dto.setMimeType(mimeType);
        dto.setSizeKb(toKilobytes(file.getSize()));

        if (dto.getMediaTypes() == null && mimeType != null) {
            dto.setMediaTypes(resolveMediaType(mimeType));
        }
    }

    private void validateUploadRequest(MediaRequestDto dto) throws BindException {
        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "metadata");
        validator.validate(dto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
    }

    private Integer resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object details = authentication.getDetails();
        if (details instanceof String userId && !userId.isBlank()) {
            try {
                return Integer.valueOf(userId);
            } catch (NumberFormatException ex) {
                logger.warn("Unable to parse authenticated user id from request details: {}", userId);
            }
        }

        return null;
    }

    private String resolveMimeType(MultipartFile file, String requestedMimeType) {
        String fileMimeType = file.getContentType();
        if (fileMimeType != null && !fileMimeType.isBlank()) {
            return fileMimeType;
        }
        return requestedMimeType;
    }

    private long toKilobytes(long sizeInBytes) {
        return Math.max(1L, (sizeInBytes + 1023L) / 1024L);
    }

    private MediaTypes resolveMediaType(String mimeType) {
        if (mimeType.startsWith("image/")) {
            return MediaTypes.IMAGE;
        }
        if (mimeType.startsWith("video/")) {
            return MediaTypes.VIDEO;
        }
        throw new InvalidMediaTypeException(mimeType);
    }
}
