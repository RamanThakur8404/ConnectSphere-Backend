package com.connectsphere.like_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Feign client for inter-service communication with post-service.
@FeignClient(name = "post-service")
public interface PostServiceClient {

    @PostMapping("/api/v1/posts/{postId}/like")
    void incrementLikeCount(@PathVariable("postId") int postId);

    @PostMapping("/api/v1/posts/{postId}/unlike")
    void decrementLikeCount(@PathVariable("postId") int postId);
}
