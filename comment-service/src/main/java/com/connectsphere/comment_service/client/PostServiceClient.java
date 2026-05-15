package com.connectsphere.comment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Feign client for inter-service communication with post-service.
@FeignClient(name = "post-service")
public interface PostServiceClient {

    @PostMapping("/api/v1/posts/{postId}/comment")
    void incrementCommentCount(@PathVariable("postId") int postId);

    @PostMapping("/api/v1/posts/{postId}/uncomment")
    void decrementCommentCount(@PathVariable("postId") int postId);
}
