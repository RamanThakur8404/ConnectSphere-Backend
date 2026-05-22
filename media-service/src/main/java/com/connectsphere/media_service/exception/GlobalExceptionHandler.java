package com.connectsphere.media_service.exception;

import com.connectsphere.media_service.config.MediaUploadProperties;
import com.connectsphere.media_service.constants.ErrorMessages;
import com.connectsphere.media_service.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.util.unit.DataSize;

import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MediaUploadProperties uploadProperties;

    public GlobalExceptionHandler(MediaUploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    // 404 handlers

    // Handles {@link MediaNotFoundException} thrown when a media record is not found.
    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaNotFound(MediaNotFoundException ex) {
        logger.warn("Media not found: mediaId={}", ex.getMediaId());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        HttpStatus.NOT_FOUND.value(),
                        ErrorMessages.MEDIA_NOT_FOUND + ex.getMediaId()));
    }

    // Handles {@link StoryNotFoundException} thrown when a story is expired or absent.
    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleStoryNotFound(StoryNotFoundException ex) {
        logger.warn("Story not found or expired: storyId={}", ex.getStoryId());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        HttpStatus.NOT_FOUND.value(),
                        ErrorMessages.STORY_NOT_FOUND + ex.getStoryId()));
    }

    // 400 handlers

    // Handles {@link InvalidMediaTypeException} when an unsupported MIME type is uploaded.
    @ExceptionHandler(InvalidMediaTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidMediaType(InvalidMediaTypeException ex) {
        logger.warn("Invalid media type submitted: {}", ex.getMimeType());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(MediaServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaServiceException(MediaServiceException ex) {
        logger.warn("Media service error: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus().value(), ex.getMessage()));
    }

    // Handles Bean Validation failures from {@code @Valid} annotations on request bodies.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        return buildValidationErrorResponse(ex.getBindingResult());
    }

    // Handles programmatic validation failures, including multipart upload metadata enrichment.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException ex) {

        return buildValidationErrorResponse(ex.getBindingResult());
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> buildValidationErrorResponse(Errors bindingErrors) {

        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : bindingErrors.getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        logger.warn("Validation failed: {}", validationErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Validation failed")
                        .data(validationErrors)
                        .build());
    }

    // 403 handler

    // Handles {@link UnauthorizedMediaAccessException} when a user tries to modify
    @ExceptionHandler(UnauthorizedMediaAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedAccess(
            UnauthorizedMediaAccessException ex) {
        logger.warn("Unauthorized media access: requesterId={}, ownerId={}",
                ex.getRequesterId(), ex.getResourceOwnerId());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    // 500 fallback

    // Handles missing required request parameters.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    // Handles missing multipart request parts such as an absent file or metadata payload.
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(
            MissingServletRequestPartException ex) {
        logger.warn("Missing request part: {}", ex.getRequestPartName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    // Handles invalid path variables and request parameters such as non-numeric IDs.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        logger.warn("Request argument type mismatch: {}={}", ex.getName(), ex.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid request parameter"));
    }

    // Handles unsupported Content-Type.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex) {
        logger.warn("Unsupported media type: {}", ex.getContentType());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage()));
    }

    // Handles malformed or unreadable request bodies.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException ex) {
        logger.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ErrorMessages.MALFORMED_REQUEST));
    }

    // Handles multipart payloads that exceed the configured upload limits.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {
        logger.warn("Rejected multipart upload because it exceeded the configured size limit");
        return uploadTooLargeResponse();
    }

    // Handles multipart parsing failures that Spring may wrap around size-limit errors.
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException ex) {
        if (isUploadSizeExceeded(ex)) {
            logger.warn("Rejected multipart upload because it exceeded the configured size limit");
            return uploadTooLargeResponse();
        }

        logger.warn("Malformed multipart request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ErrorMessages.MALFORMED_REQUEST));
    }

    // Catch-all handler for any unexpected exception not handled by a more specific handler.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        if (isUploadSizeExceeded(ex)) {
            logger.warn("Rejected multipart upload because it exceeded the configured size limit");
            return uploadTooLargeResponse();
        }

        logger.error("Unhandled exception in media-service", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ErrorMessages.INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> uploadTooLargeResponse() {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(
                        HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        ErrorMessages.MAX_UPLOAD_SIZE_EXCEEDED + " Maximum allowed sizes are "
                                + format(uploadProperties.getMaxFileSize()) + " per file and "
                                + format(uploadProperties.getMaxRequestSize()) + " per request."));
    }

    private boolean isUploadSizeExceeded(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof MaxUploadSizeExceededException
                    || (message != null && (message.contains("maximum permitted size")
                    || message.contains("exceeds the configured maximum")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String format(DataSize size) {
        return size.toMegabytes() + "MB";
    }
}
