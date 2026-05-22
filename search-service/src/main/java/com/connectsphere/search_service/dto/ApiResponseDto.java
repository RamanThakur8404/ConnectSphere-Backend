package com.connectsphere.search_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Standardised API response envelope for all endpoints.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDto<T> {

	private boolean success;
	private String message;
	private T data;
	private LocalDateTime timestamp;

	// Factory for success responses. 
	public static <T> ApiResponseDto<T> ok(String message, T data) {
		return ApiResponseDto.<T>builder().success(true).message(message).data(data).timestamp(LocalDateTime.now())
				.build();
	}

	// Factory for error responses. 
	public static <T> ApiResponseDto<T> error(String message) {
		return ApiResponseDto.<T>builder().success(false).message(message).timestamp(LocalDateTime.now()).build();
	}
}
