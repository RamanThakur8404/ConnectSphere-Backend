package com.connectsphere.media_service.exception;

import lombok.Getter;

// Thrown when a requested story cannot be found, is expired, or has been
public class StoryNotFoundException extends RuntimeException {
	@Getter
	private final int storyId;

	// Constructs the exception with a descriptive message.
	public StoryNotFoundException(int storyId) {
		super("Story not found or has expired with id: " + storyId);
		this.storyId = storyId;
	}
}