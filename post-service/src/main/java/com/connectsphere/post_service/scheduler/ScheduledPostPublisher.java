package com.connectsphere.post_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.post_service.entity.Post;
import com.connectsphere.post_service.repository.PostRepository;

import lombok.RequiredArgsConstructor;

// Scheduled job to publish posts that have a scheduledPublishAt timestamp in the past.
@Component
@ConditionalOnProperty(name = "posts.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ScheduledPostPublisher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostPublisher.class);

    private final PostRepository postRepository;

    // Runs every 60 seconds. Finds posts whose scheduledPublishAt is now in the past
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void publishDuePosts() {
        List<Post> duePosts = postRepository.findDueScheduledPosts(LocalDateTime.now());

        if (duePosts.isEmpty()) {
            return;
        }

        log.info("ScheduledPostPublisher — publishing {} due posts", duePosts.size());

        for (Post post : duePosts) {
            post.setScheduledPublishAt(null);
            postRepository.save(post);
            log.info("Scheduled post published — postId={}, authorId={}", post.getPostId(), post.getAuthorId());
        }
    }
}
