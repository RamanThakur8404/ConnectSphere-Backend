package com.connectsphere.comment_service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleCommentNotFound")
    void handleCommentNotFound() {
        CommentNotFoundException ex = new CommentNotFoundException(1L);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCommentNotFound(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError()).isEqualTo("COMMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("handleUnauthorizedAccess")
    void handleUnauthorizedAccess() {
        UnauthorizedCommentAccessException ex = new UnauthorizedCommentAccessException("Unauthorized");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedAccess(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("handleInvalidOperation")
    void handleInvalidOperation() {
        InvalidCommentOperationException ex = new InvalidCommentOperationException("Invalid");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidOperation(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("INVALID_OPERATION");
    }

    @Test
    @DisplayName("handleValidationErrors")
    void handleValidationErrors() throws NoSuchMethodException {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "message");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        // Use a real MethodParameter to avoid NPE from getExecutable()
        MethodParameter methodParameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("handleValidationErrors"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getFieldErrors()).containsEntry("field", "message");
    }

    @Test
    @DisplayName("handleIllegalArgument")
    void handleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("illegal");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("ILLEGAL_ARGUMENT");
    }

    @Test
    @DisplayName("handleGenericException")
    void handleGenericException() {
        Exception ex = new Exception("Generic");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
