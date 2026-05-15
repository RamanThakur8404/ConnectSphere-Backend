package com.connectsphere.auth.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_ShouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found: test@example.com");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("User not found: test@example.com", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleDuplicate_ShouldReturn409() {
        DuplicateResourceException ex = new DuplicateResourceException("Email already exists");

        ResponseEntity<Map<String, Object>> response = handler.handleDuplicate(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Email already exists", response.getBody().get("message"));
    }

    @Test
    void handleAuthFailed_ShouldReturn401() {
        AuthenticationFailedException ex = new AuthenticationFailedException("Invalid credentials");

        ResponseEntity<Map<String, Object>> response = handler.handleAuthFailed(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().get("status"));
        assertEquals("Invalid credentials", response.getBody().get("message"));
    }

    @Test
    void handleIllegalArg_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Cannot create USER via admin endpoint");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArg(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Cannot create USER via admin endpoint", response.getBody().get("message"));
    }

    @Test
    void handleGeneral_ShouldReturn500() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }
}
