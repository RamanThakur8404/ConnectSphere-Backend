package com.connectsphere.notification_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.connectsphere.notification_service.dto.ApiResponse;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound() {
        NotificationNotFoundException ex = new NotificationNotFoundException(1);
        ResponseEntity<ApiResponse<?>> resp = handler.handleNotFound(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().isSuccess()).isFalse();
    }

    @Test
    void handleBulk() {
        BulkNotificationException ex = new BulkNotificationException("empty");
        ResponseEntity<ApiResponse<?>> resp = handler.handleBulk(ex);
        assertThat(resp.getBody().isSuccess()).isFalse();
    }

    @Test
    void handleEmail() {
        EmailNotificationException ex = new EmailNotificationException("fail", "a@b.com", new RuntimeException());
        ResponseEntity<ApiResponse<?>> resp = handler.handleEmail(ex);
        assertThat(resp.getBody().isSuccess()).isFalse();
    }

    @Test
    void handlePersistence() {
        NotificationPersistenceException ex = new NotificationPersistenceException("persist fail", new RuntimeException());
        ResponseEntity<ApiResponse<?>> resp = handler.handlePersistence(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleServiceException() {
        NotificationServiceException ex = new NotificationServiceException("generic", HttpStatus.BAD_REQUEST) {};
        ResponseEntity<ApiResponse<?>> resp = handler.handleServiceException(ex);
        assertThat(resp.getBody().isSuccess()).isFalse();
    }

    @Test
    void handleIllegalArgument() {
        ResponseEntity<ApiResponse<?>> resp = handler.handleIllegalArgument(new IllegalArgumentException("bad"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleGeneric() {
        ResponseEntity<ApiResponse<?>> resp = handler.handleGeneric(new Exception("oops"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
