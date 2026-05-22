package com.connectsphere.comment_service.exception;
public class InvalidCommentOperationException extends RuntimeException {
 
    public InvalidCommentOperationException(String message) {
        super(message);
    }
}