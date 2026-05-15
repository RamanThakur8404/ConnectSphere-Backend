package com.connectsphere.media_service.exception;

import lombok.Getter;

// Thrown when a requested media record cannot be found or has been

public class MediaNotFoundException extends RuntimeException {
	@Getter
	private final int mediaId;

	// Constructs the exception with a descriptive message containing the ID.
	public MediaNotFoundException(int mediaId) {
		super("Media not found with id: " + mediaId);
		this.mediaId = mediaId;
	}

	// Returns the media ID that triggered the exception.
}