package com.connectsphere.search_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    void handleHashtagNotFound() {
        HashtagNotFoundException ex = new HashtagNotFoundException("not found");
        ResponseEntity<?> resp = handler.handleHashtagNotFound(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleInvalidSearchQuery() {
        InvalidSearchQueryException ex = new InvalidSearchQueryException("blank");
        ResponseEntity<?> resp = handler.handleInvalidQuery(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleGenericException() {
        Exception ex = new Exception("oops");
        ResponseEntity<?> resp = handler.handleUnexpected(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
