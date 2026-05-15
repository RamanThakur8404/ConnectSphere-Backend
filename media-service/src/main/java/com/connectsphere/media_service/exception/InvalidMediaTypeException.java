package com.connectsphere.media_service.exception;

import lombok.Getter;

// Thrown when a client attempts to upload a file whose MIME type is not
public class InvalidMediaTypeException extends RuntimeException {
	@Getter
	private final String mimeType;

	// Constructs the exception with the rejected MIME type in the message.
	public InvalidMediaTypeException(String mimeType) {
		super("Unsupported media type: '" + mimeType
				+ "'. Allowed types are: image/jpeg, image/png, image/webp, video/mp4");
		this.mimeType = mimeType;
	}
}