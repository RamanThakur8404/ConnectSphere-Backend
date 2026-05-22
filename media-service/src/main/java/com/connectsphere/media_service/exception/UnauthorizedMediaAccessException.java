package com.connectsphere.media_service.exception;

import lombok.Getter;

// Thrown when a user attempts to modify or delete a media record or story that
@Getter
public class UnauthorizedMediaAccessException extends RuntimeException {

	private final int requesterId;
	private final int resourceOwnerId;

	// Constructs the exception with context about the requester and owner.
	public UnauthorizedMediaAccessException(int requesterId, int resourceOwnerId) {
		super("User " + requesterId + " is not authorized to modify media owned by user " + resourceOwnerId);
		this.requesterId = requesterId;
		this.resourceOwnerId = resourceOwnerId;
	}
}