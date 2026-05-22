package com.connectsphere.post_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

// Feign client for inter-service communication with search-service.
@FeignClient(name = "search-service")
public interface SearchServiceClient {

    @PostMapping("/api/v1/search/index")
    void indexPost(@RequestHeader("X-User-Id") String userId,
                   @RequestHeader("X-User-Role") String userRole,
                   @RequestBody Map<String, Object> indexRequest);

    @DeleteMapping("/api/v1/search/index/{postId}")
    void removePostIndex(@RequestHeader("X-User-Id") String userId,
                         @RequestHeader("X-User-Role") String userRole,
                         @PathVariable("postId") int postId);
}
