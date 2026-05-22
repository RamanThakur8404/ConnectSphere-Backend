package com.connectsphere.search_service.constant;

public final class LogMessages {

	private LogMessages() {
		// constants-only class
	}

	// Post Indexing
	public static final String INDEX_POST_ATTEMPT = "Indexing hashtags for post [postId={}]";
	public static final String INDEX_POST_NO_TAGS = "No hashtags found in post content [postId={}]";
	public static final String INDEX_POST_SUCCESS = "Indexed {} hashtag(s) for post [postId={}]: {}";
	public static final String INDEX_POST_UPSERT_TAG = "Upserting hashtag [tag={}] for post [postId={}]";

	// Post Index Removal
	public static final String REMOVE_INDEX_ATTEMPT = "Removing hashtag index for post [postId={}]";
	public static final String REMOVE_INDEX_SUCCESS = "Hashtag index removed for post [postId={}]";

	// Post Search
	public static final String SEARCH_POSTS_ATTEMPT = "Searching posts for keyword [{}]";
	public static final String SEARCH_POSTS_SUCCESS = "Found {} post(s) matching keyword [{}]";
	public static final String SEARCH_POSTS_EMPTY = "No posts found for keyword [{}]";

	// User Search
	public static final String SEARCH_USERS_ATTEMPT = "User search requested [query={}]";
	public static final String SEARCH_USERS_SUCCESS = "User search completed [query={}, results={}]";

	// Hashtag Operations
	public static final String HASHTAG_FETCH_ATTEMPT = "Fetching hashtags for post [postId={}]";
	public static final String HASHTAG_FETCH_SUCCESS = "Retrieved {} hashtag(s) for post [postId={}]";
	public static final String HASHTAG_POSTS_ATTEMPT = "Fetching posts for hashtag [#{}]";
	public static final String HASHTAG_POSTS_SUCCESS = "Found {} post(s) for hashtag [#{}]";
	public static final String HASHTAG_SEARCH_ATTEMPT = "Hashtag autocomplete search [fragment={}]";
	public static final String HASHTAG_SEARCH_SUCCESS = "Hashtag autocomplete returned {} result(s) [fragment={}]";
	public static final String HASHTAG_COUNT_ATTEMPT = "Fetching post count for hashtag [#{}]";
	public static final String HASHTAG_COUNT_SUCCESS = "Hashtag [#{}] has {} post(s)";

	// Trending
	public static final String TRENDING_ATTEMPT = "Fetching top {} trending hashtags since [{}]";
	public static final String TRENDING_SUCCESS = "Returned {} trending hashtag(s)";

	// RabbitMQ Consumers
	public static final String RABBITMQ_INDEX_RECEIVED = "RabbitMQ: received index-post event [postId={}]";
	public static final String RABBITMQ_REMOVE_RECEIVED = "RabbitMQ: received remove-index event [postId={}]";
	public static final String RABBITMQ_INDEX_ERROR = "RabbitMQ: failed to process index event [postId={}]: {}";
}
