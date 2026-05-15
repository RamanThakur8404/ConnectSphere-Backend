package com.connectsphere.like_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleLikeNotFound() {
        LikeNotFoundException ex = new LikeNotFoundException("not found");
        ResponseEntity<?> resp = handler.handleLikeNotFound(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleDuplicateLike() {
        DuplicateLikeException ex = new DuplicateLikeException("duplicate");
        ResponseEntity<?> resp = handler.handleDuplicateLike(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleInvalidTargetType() {
        InvalidTargetTypeException ex = new InvalidTargetTypeException("invalid");
        ResponseEntity<?> resp = handler.handleInvalidTargetType(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("integrity");
        ResponseEntity<?> resp = handler.handleDataIntegrityViolation(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleGenericException() {
        Exception ex = new Exception("oops");
        ResponseEntity<?> resp = handler.handleGenericException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
