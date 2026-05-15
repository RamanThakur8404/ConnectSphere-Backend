package com.connectsphere.search_service.constant;

public final class ErrorMessages {

	private ErrorMessages( ) {}
	
	// Hashtag lookup
	public static final String HASHTAG_NOT_FOUND = "Hashtag not found: #"; // append tag at call site
	public static final String HASHTAG_BLANK = "Hashtag must not be blank";

	// Search query
	public static final String SEARCH_QUERY_BLANK = "Search keyword must not be blank";
	public static final String INVALID_POST_ID = "Post ID must be a positive integer";

	// Access control
	public static final String ACCESS_DENIED = "Access denied: insufficient permissions";
	public static final String UNAUTHORIZED = "Authentication required";
}
