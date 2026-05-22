package com.connectsphere.like_service.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateLikeException extends RuntimeException {

	public DuplicateLikeException(String message) {
		super(message);
	}
}