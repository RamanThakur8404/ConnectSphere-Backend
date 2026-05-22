package com.connectsphere.media_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

// Generic API response wrapper used across all endpoints.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // HTTP status code reflected in the body for client convenience. 
    private int status;

    // Human-readable message summarising the outcome. 
    private String message;

    // The actual response payload (null on error responses). 
    private T data;

    // Timestamp at which this response was generated. 
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Factory method for a successful response with a data payload.
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    // Factory method for an error response without a data payload.
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .build();
    }
}
