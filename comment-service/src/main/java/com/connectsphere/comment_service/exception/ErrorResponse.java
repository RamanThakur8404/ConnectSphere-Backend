package com.connectsphere.comment_service.exception;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
	private int status;
	private String error;
	private String message;
	private LocalDateTime timestamp;

	private Map<String, String> fieldErrors;
	
	public ErrorResponse(int status, String error, String message) {
		this.status = status;
		this.error= error;
		this.message = message;
		this.timestamp = LocalDateTime.now();
	}
}
