package com.connectsphere.media_service.scheduler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.connectsphere.media_service.service.MediaService;

import lombok.RequiredArgsConstructor;

// Scheduled job that expires stories older than 24 hours.
@Component
@RequiredArgsConstructor
public class StoryExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(StoryExpiryScheduler.class);

    private final MediaService mediaService;


    // Fires every 5 minutes to expire all overdue stories.
    @Scheduled(fixedRateString = "${story.expiry.interval.ms:300000}")
    public void expireStories() {
        logger.info("Story expiry scheduler triggered");
        try {
            int count = mediaService.expireOldStories();
            logger.info("Story expiry run complete. Expired stories: {}", count);
        } catch (Exception ex) {
            // Log but do not propagate — scheduler must keep running
            logger.error("Error during story expiry run", ex);
        }
    }
}
