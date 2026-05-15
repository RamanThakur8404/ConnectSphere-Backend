package com.connectsphere.like_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Feign client for inter-service communication with comment-service.
@FeignClient(name = "comment-service")
public interface CommentServiceClient {

    @PostMapping("/api/v1/comments/{commentId}/like")
    void incrementLikeCount(@PathVariable("commentId") Long commentId);

    @PostMapping("/api/v1/comments/{commentId}/unlike")
    void decrementLikeCount(@PathVariable("commentId") Long commentId);
}
