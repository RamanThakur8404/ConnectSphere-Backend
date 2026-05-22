package com.connectsphere.like_service.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTargetTypeException extends RuntimeException {

	public InvalidTargetTypeException(String message) {
		super(message);
	}
}
