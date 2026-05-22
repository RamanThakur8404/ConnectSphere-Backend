package com.connectsphere.media_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.connectsphere.media_service.config.MediaUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        MediaUploadProperties uploadProperties = new MediaUploadProperties();
        uploadProperties.setMaxFileSize(DataSize.ofMegabytes(50));
        uploadProperties.setMaxRequestSize(DataSize.ofMegabytes(60));
        handler = new GlobalExceptionHandler(uploadProperties);
    }

    @Test
    void handleMediaNotFound() {
        MediaNotFoundException ex = new MediaNotFoundException(1);
        ResponseEntity<?> resp = handler.handleMediaNotFound(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleStoryNotFound() {
        StoryNotFoundException ex = new StoryNotFoundException(1);
        ResponseEntity<?> resp = handler.handleStoryNotFound(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleInvalidMediaType() {
        InvalidMediaTypeException ex = new InvalidMediaTypeException("bad/type");
        ResponseEntity<?> resp = handler.handleInvalidMediaType(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleUnauthorizedAccess() {
        UnauthorizedMediaAccessException ex = new UnauthorizedMediaAccessException(1, 2);
        ResponseEntity<?> resp = handler.handleUnauthorizedAccess(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleMissingParam() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("param", "String");
        ResponseEntity<?> resp = handler.handleMissingParam(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleMissingPart() {
        MissingServletRequestPartException ex = new MissingServletRequestPartException("part");
        ResponseEntity<?> resp = handler.handleMissingPart(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("bad");
        ResponseEntity<?> resp = handler.handleUnsupportedMediaType(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void handleUnreadableMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad");
        ResponseEntity<?> resp = handler.handleUnreadableMessage(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleMaxUploadSizeExceeded() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1_048_576L);
        ResponseEntity<?> resp = handler.handleMaxUploadSizeExceeded(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(resp.getBody()).hasFieldOrPropertyWithValue(
                "message",
                "Uploaded file exceeds the allowed size limit Maximum allowed sizes are 50MB per file and 60MB per request.");
    }

    @Test
    void handleMultipartException_uploadLimitExceeded() {
        MultipartException ex = new MultipartException(
                "the request was rejected because its size (48172890) exceeds the configured maximum (10485760)");

        ResponseEntity<?> resp = handler.handleMultipartException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void handleGenericException() {
        ResponseEntity<?> resp = handler.handleGenericException(new Exception("oops"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
