package com.connectsphere.media_service.constants;

public final class LogMessages {

	private LogMessages() {
		// constants-only class  
	}
		

	// Media Upload
	public static final String UPLOAD_ATTEMPT = "Uploading media — uploaderId: {}, mimeType: {}";
	public static final String UPLOAD_SUCCESS = "Media persisted — mediaId: {}";
	public static final String UPLOAD_FAILED = "Failed to persist media — uploaderId: {}";

	// Media Retrieval
	public static final String GET_MEDIA_BY_ID = "Fetching media — mediaId: {}";
	public static final String GET_MEDIA_BY_POST = "Fetching media for postId: {}";

	// Media Delete
	public static final String DELETE_MEDIA_ATTEMPT = "Soft-deleting media — mediaId: {}";
	public static final String DELETE_MEDIA_SUCCESS = "Media soft-deleted — mediaId: {}";
	public static final String DELETE_MEDIA_NOT_FOUND = "Media delete failed — not found: {}";

	// Story Create
	public static final String STORY_CREATE_ATTEMPT = "Creating story — authorId: {}";
	public static final String STORY_CREATE_SUCCESS = "Story created — storyId: {}, expiresAt: {}";
	public static final String STORY_CREATE_FAILED = "Failed to persist story — authorId: {}";

	// Story Retrieval
	public static final String GET_ACTIVE_STORIES = "Fetching active stories for {} authors";
	public static final String GET_STORIES_BY_USER = "Fetching stories for authorId: {}";

	// Story View
	public static final String STORY_VIEW_ATTEMPT = "Recording story view — storyId: {}, viewerId: {}";
	public static final String STORY_VIEW_SUCCESS = "Story view recorded — storyId: {}";

	// Story Delete
	public static final String STORY_DELETE_ATTEMPT = "Soft-deleting story — storyId: {}";
	public static final String STORY_DELETE_SUCCESS = "Story deactivated — storyId: {}";
	public static final String STORY_DELETE_NOT_FOUND = "Story delete failed — not found or expired: {}";

	// Story Expiry Scheduler
	public static final String EXPIRY_RUN_START = "Story expiry scheduler triggered";
	public static final String EXPIRY_RUN_COMPLETE = "Story expiry run complete — expired: {}";
	public static final String EXPIRY_RUN_ERROR = "Error during story expiry run";

	// Security / Headers
	public static final String HEADER_AUTH_SUCCESS = "Gateway-header auth set — userId: {}, role: {}";
	public static final String HEADER_AUTH_MISSING = "No X-User-Id header on request to: {}";

	// RabbitMQ Events
	public static final String EVENT_MEDIA_PUBLISHED = "Published media.uploaded event — mediaId: {}";
	public static final String EVENT_STORY_PUBLISHED = "Published story.created event — storyId: {}";
}
