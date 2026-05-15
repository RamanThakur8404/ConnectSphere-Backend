package com.connectsphere.comment_service.exception;
public class UnauthorizedCommentAccessException extends RuntimeException {
 
    public UnauthorizedCommentAccessException(int userId, int commentId) {
        super("User " + userId + " is not authorized to modify comment " + commentId);
    }
 
    public UnauthorizedCommentAccessException(String message) {
        super(message);
    }
}